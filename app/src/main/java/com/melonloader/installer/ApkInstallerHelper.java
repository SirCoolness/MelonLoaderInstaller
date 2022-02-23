package com.melonloader.installer;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Trace;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import com.android.apksig.ApkVerifier;

import java.io.File;
import java.security.Signature;
import java.util.List;

public class ApkInstallerHelper {
    Activity context;
    String packageName;

    String pending = null;
    Runnable next = null;

    public ApkInstallerHelper(Activity _context, String _packageName)
    {
        context = _context;
        packageName = _packageName;
    }

    public void InstallApk(String path)
    {
        next = () -> InternalInstall(path);
        UninstallPackage();
    }

    protected void InternalInstall(String path)
    {
        AsyncTask.execute(() -> {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
                return;
            }

            context.runOnUiThread(() -> {
                Uri filePath = uriFromFile(context, new File(path));

                Intent install = new Intent(Intent.ACTION_VIEW);
                install.setDataAndType(filePath, "application/vnd.android.package-archive");

                install.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                install.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                try {
                    context.startActivity(install);
                } catch (ActivityNotFoundException e) {
                    e.printStackTrace();
                    Log.e("TAG", "Error in opening the file!");
                }
            });
        });
    }

    protected void UninstallPackage()
    {
        context.runOnUiThread(() -> {
            pending = Intent.ACTION_DELETE;

            Intent intent = new Intent(Intent.ACTION_DELETE);
            intent.setData(Uri.parse("package:" + packageName));
            context.startActivityForResult(intent, 1000);
        });
    }

    private static Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, BuildConfig.APPLICATION_ID + ".provider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)
    {
        Log.e("melonloader", "" + requestCode + " " + resultCode);
//        if (!data.getAction().equals(pending))
//            return;

        if (requestCode != 1000)
            return;

        pending = null;

        if (next != null)
            next.run();

        next = null;
    }
}
