package com.melonloader.installer;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;

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
}
