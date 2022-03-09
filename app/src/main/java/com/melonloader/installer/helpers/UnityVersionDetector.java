package com.melonloader.installer.helpers;

import android.content.res.AssetManager;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class UnityVersionDetector extends com.melonloader.installer.core.UnityVersionDetector {
    private AssetManager assetManager;

    public UnityVersionDetector(AssetManager assetManager) {
        super(null, null);

        this.assetManager = assetManager;
    }

    @Override
    protected InputStream getStream(String local_path) {
        try {
            return assetManager.open("bin/Data/" + local_path);
        } catch (IOException e) {
            return null;
        }
    }
}
