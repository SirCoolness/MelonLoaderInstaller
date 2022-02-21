package com.melonloader.installer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class ApplicationFinder {
    private static final String TAG = "melonloader";

    public static List<SupportedApplication> GetSupportedApplications(Context context)
    {
        final PackageManager pm = context.getPackageManager();

        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        List<SupportedApplication> supportedApplications = new ArrayList<>();

        for (ApplicationInfo packageInfo : packages) {
            Intent intent = pm.getLaunchIntentForPackage(packageInfo.packageName);
            if (intent == null || !intent.getComponent().getClassName().equals("com.unity3d.player.UnityPlayerActivity"))
                continue;

            supportedApplications.add(new SupportedApplication(pm, packageInfo));
        }

        return supportedApplications;
    }

    public static SupportedApplication GetPackage(Context context, String packageName) throws PackageManager.NameNotFoundException {
        final PackageManager pm = context.getPackageManager();

        ApplicationInfo packageInfo = pm.getApplicationInfo(packageName, PackageManager.GET_META_DATA);

        return new SupportedApplication(pm, packageInfo);
    }
}
