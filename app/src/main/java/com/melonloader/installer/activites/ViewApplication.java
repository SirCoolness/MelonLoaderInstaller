package com.melonloader.installer.activites;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.melonloader.installer.ApkInstallerHelper;
import com.melonloader.installer.ApplicationFinder;
import com.melonloader.installer.R;
import com.melonloader.installer.SupportedApplication;
import com.melonloader.installer.core.ILogger;
import com.melonloader.installer.core.Main;
import com.melonloader.installer.core.Properties;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ViewApplication extends AppCompatActivity implements View.OnClickListener {
    private SupportedApplication application;
    private LoggerHelper loggerHelper;
    private ApkInstallerHelper installerHelper = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_application);

        String targetPackageName = getIntent().getStringExtra("target.packageName");
        if (targetPackageName == null) {
            finish();
            return;
        }

        try {
            application = ApplicationFinder.GetPackage(this, targetPackageName);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            finish();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayHomeAsUpEnabled(true);
//    }
//
//    @Override
//    protected void onStart() {
//        super.onStart();

        Log.e("melonloader", "on create");

        ImageView appIcon = findViewById(R.id.applicationIcon);
        TextView appName = findViewById(R.id.applicationName);
        Button patchButton = findViewById(R.id.patchButton);
        patchButton.setOnClickListener(this);
        patchButton.setEnabled(!application.patched);
        patchButton.setText(application.patched ? "PATCHED" : "PATCH");

        appIcon.setImageDrawable(application.icon);
        appName.setText(application.appName);

        loggerHelper = new LoggerHelper(this);
    }

    @Override
    public void onClick(View view) {
        if (application.patched) {
            Toast.makeText(this, "Application already patched.", Toast.LENGTH_SHORT).show();
            return;
        }

        StartPatching();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();

        if (id==android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void StartPatching()
    {
        loggerHelper.Clear();
        String depsLocation = Paths.get(getExternalFilesDir(null).toString(), "temp", "dependencies.zip").toString();
        String unityAssetsLocation = Paths.get(getExternalFilesDir(null).toString(), "temp", "unity.zip").toString();

//        String zipAlignLocation = Paths.get("/data", "data", "com.melonloader.installer", "ml-zipalign").toString();
        String zipAlignLocation = Paths.get(getFilesDir().toString(), "ml-zipalign").toString();

        String keystoreLocation = Paths.get(getExternalFilesDir(null).toString(), "temp", "melonloader.keystore").toString();

        Button patchButton = findViewById(R.id.patchButton);

        AsyncTask.execute(() -> {
            runOnUiThread(() -> {
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayHomeAsUpEnabled(false);
                patchButton.setEnabled(false);
                patchButton.setText("PATCHING");
            });

            loggerHelper.Log("Preparing Assets");

            copyAssets("installer_deps.zip", depsLocation);
            copyAssets("unity_libs.zip", unityAssetsLocation);
            copyAssets("zipalign", zipAlignLocation);
            copyAssets("cert.bks", keystoreLocation);

            loggerHelper.Log("Preparing Exectables");
            makeExecutable(zipAlignLocation);

            loggerHelper.Log("Starting patch");

            Path tempPath = Paths.get(getExternalFilesDir(null).toString(), "temp", application.appName);
            boolean success = Main.Run(new Properties() {{
                targetApk = application.apkLocation;
                tempDir = tempPath.toString();
                logger = loggerHelper;
                dependencies = depsLocation;
                unityArchive = unityAssetsLocation;
                zipAlign = zipAlignLocation;
                keystore = keystoreLocation;
                keystorePass = "123456";
            }});

            if (success) {
                loggerHelper.Log("Application Successfully patched. Reinstalling.");

                runOnUiThread(() -> {
                    installerHelper = new ApkInstallerHelper(this, application.appName);
                    installerHelper.InstallApk(Paths.get(tempPath.toString(), "base.apk").toString());
                });
            }

            runOnUiThread(() -> {
                ActionBar actionBar = getSupportActionBar();
                actionBar.setDisplayHomeAsUpEnabled(true);
                patchButton.setText(success ? "PATCHED" : "FAILED");

                loggerHelper.scroller.fullScroll(ScrollView.FOCUS_DOWN);
            });
        });
    }

    private void copyAssets(String assetName, String dest) {
        AssetManager assetManager = getAssets();
        String[] files = null;
        try {
            files = assetManager.list("");
        } catch (IOException e) {
            loggerHelper.Log("Failed to get asset file list. -> " + e.toString());
        }

        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(assetName);
            File outFile = new File(dest);
            out = new FileOutputStream(outFile);
            copyFile(in, out);

            in.close();
            in = null;

            out.flush();
            out.close();
            out = null;
        } catch(IOException e) {
            loggerHelper.Log("Failed to copy asset file: " + assetName + " -> " + e);
        }
    }

    private boolean makeExecutable(String path)
    {
        File myFile = new File(path);

        if (!myFile.canExecute()) {
            loggerHelper.Log("[" + path + "] Trying to make executable.");
            if (!myFile.setExecutable(true)) {
                loggerHelper.Log("[" + path + "] Failed to make exectuable.");
                return false;
            };
        }

        return true;
    }

    private void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    class LoggerHelper implements ILogger {
        TextView content;
        ScrollView scroller;
        boolean dirty = false;

        public LoggerHelper(Activity context)
        {
            content = context.findViewById(R.id.loggerBody);
            scroller = context.findViewById(R.id.loggerScroll);
            content.setText("");
        }

        public void Clear()
        {
            runOnUiThread(() -> {
                content.setText("");
            });
        }

        public void Log(String msg)
        {
            Log.i("melonloader", msg);

            runOnUiThread(() -> {
                if (dirty)
                    content.append("\n");
                else
                    dirty = true;

                content.append(msg);
                scroller.fullScroll(ScrollView.FOCUS_DOWN);
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (installerHelper != null)
            installerHelper.onActivityResult(requestCode, resultCode, data);
    }
}