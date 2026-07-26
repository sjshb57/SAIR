package com.aefyr.sai.installer2.base.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aefyr.sai.model.apksource.ApkSource;

public class SaiPiSessionParams {

    private final ApkSource mApkSource;

    /** Known upfront from the parsed archive; shell installers cannot learn it from pm. */
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
