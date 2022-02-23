package com.melonloader.installer;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

import androidx.loader.content.AsyncTaskLoader;

import com.melonloader.installer.core.Main;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class SupportedApplication {
    private ApplicationInfo application;
    public Drawable icon;
    public boolean patched;
    public String appName;
    public String apkLocation;
    public String packageName;
    public String unityVersion;
    private boolean getVersionAttempted = false;

    public SupportedApplication(PackageManager pm, ApplicationInfo info)
    {
        application = info;
        icon = info.loadIcon(pm);
        appName = info.packageName;
        packageName = info.packageName;
        apkLocation = info.publicSourceDir;

        CheckPatched();
    }

    public void CheckPatched()
    {
        patched = Main.IsPatched(application.publicSourceDir);
    }

    public void TryDetectVersion(String tempDir)
    {
        TryDetectVersion(tempDir, () -> {});
    }

    public void TryDetectVersion(String tempDir, Runnable callback)
    {
        if (getVersionAttempted)
            return;

        getVersionAttempted = true;

        try {
            Files.createDirectories(Paths.get(tempDir));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        AsyncTask.execute(() -> {
            unityVersion = Main.DetectUnityVersion(apkLocation, tempDir);

            if (unityVersion == null) {
                return;
            }

            callback.run();
        });
    }
}
