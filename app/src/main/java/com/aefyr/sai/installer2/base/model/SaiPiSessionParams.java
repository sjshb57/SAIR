package com.aefyr.sai.installer2.base.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aefyr.sai.model.apksource.ApkSource;

public class SaiPiSessionParams {

    private final ApkSource mApkSource;

    /**
     * Package name resolved while parsing the archive, when it is known upfront.
     * <p>
     * Shell based installers drive the installation through pm, which does not report back which
     * package was installed, so this is what lets a finished session show the app name and icon
     * instead of the raw file name.
     */
    @Nullable
    private final String mPackageName;

    public SaiPiSessionParams(@NonNull ApkSource apkSource) {
        this(apkSource, null);
    }

    public SaiPiSessionParams(@NonNull ApkSource apkSource, @Nullable String packageName) {
        mApkSource = apkSource;
        mPackageName = packageName;
    }

    @NonNull
    public ApkSource apkSource() {
        return mApkSource;
    }

    @Nullable
    public String packageName() {
        return mPackageName;
    }
}
