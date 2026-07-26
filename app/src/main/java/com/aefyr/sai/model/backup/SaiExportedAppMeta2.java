package com.aefyr.sai.model.backup;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.aefyr.sai.utils.Utils;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import androidx.core.content.pm.PackageInfoCompat;
import android.content.pm.ApplicationInfo;

//TODO add validation
public class SaiExportedAppMeta2 {

    public static final String META_FILE = "meta.sai_v2.json";
    public static final String ICON_FILE = SaiExportedAppMeta.ICON_FILE;

    @SerializedName("package")
    @Expose
    private String mPackage;

    @SerializedName("label")
    @Expose
    private String mLabel;

    @SerializedName("version_name")
    @Expose
    private String mVersionName;

    @SerializedName("version_code")
    @Expose
    private Long mVersionCode;

    @SerializedName("export_timestamp")
    @Expose
    private Long mExportTimestamp;

    @Nullable
    @SerializedName("min_sdk")
    @Expose
    private Long mMinSdk;

    @Nullable
    @SerializedName("target_sdk")
    @Expose
    private Long mTargetSdk;

    @Nullable
    @SerializedName("backup_components")
    @Expose
    private List<BackupComponent> mBackupComponents;

    @SerializedName("split_apk")
    @Expose
    private boolean mIsSplitApk;

    private SaiExportedAppMeta2() {

    }

    public static SaiExportedAppMeta2 createForPackage(Context context, String pkg, long exportTimestamp) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(pkg, 0);

        SaiExportedAppMeta2 appMeta = new SaiExportedAppMeta2();
        appMeta.mPackage = packageInfo.packageName;
        appMeta.mLabel = packageInfo.applicationInfo != null
                ? packageInfo.applicationInfo.loadLabel(pm).toString()
                : packageInfo.packageName;
        appMeta.mVersionName = packageInfo.versionName;

        if (Utils.apiIsAtLeast(Build.VERSION_CODES.P)) {
            appMeta.mVersionCode = packageInfo.getLongVersionCode();
        } else {
            appMeta.mVersionCode = PackageInfoCompat.getLongVersionCode(packageInfo);
        }

        appMeta.mExportTimestamp = exportTimestamp;

        ApplicationInfo applicationInfo = packageInfo.applicationInfo;
        if (applicationInfo != null) {
            appMeta.mMinSdk = (long) applicationInfo.minSdkVersion;
            appMeta.mTargetSdk = (long) applicationInfo.targetSdkVersion;
            appMeta.mIsSplitApk = applicationInfo.splitPublicSourceDirs != null
                    && applicationInfo.splitPublicSourceDirs.length > 0;
        }

        return appMeta;
    }

    public SaiExportedAppMeta2 addBackupComponent(String type, long size) {
        if (mBackupComponents == null)
            mBackupComponents = new ArrayList<>();

        mBackupComponents.add(new BackupComponent(type, size));
        return this;
    }

    public static SaiExportedAppMeta2 deserialize(byte[] serializedMeta) {
        return new Gson().fromJson(new String(serializedMeta, StandardCharsets.UTF_8), SaiExportedAppMeta2.class);
    }

    public long metaVersion() {
        return 2L;
    }

    public String packageName() {
        return mPackage;
    }

    public String label() {
        return mLabel;
    }

    public String versionName() {
        return mVersionName;
    }

    public long versionCode() {
        return mVersionCode != null ? mVersionCode : 0;
    }

    public long exportTime() {
        return mExportTimestamp != null ? mExportTimestamp : 0;
    }

    @Nullable
    public Long minSdk() {
        return mMinSdk;
    }

    @Nullable
    public Long targetSdk() {
        return mTargetSdk;
    }

    public boolean isSplitApk() {
        return mIsSplitApk;
    }

    @Nullable
    public List<BackupComponent> backupComponents() {
        return mBackupComponents;
    }

    public byte[] serialize() {
        return new Gson().toJson(this).getBytes(StandardCharsets.UTF_8);
    }

    public static class BackupComponent {

        @SerializedName("type")
        @Expose
        private final String mType;

        @SerializedName("size")
        @Expose
        private final Long mSize;

        private BackupComponent(String type, long size) {
            mType = type;
            mSize = size;
        }

        @NonNull
        public String type() {
            return Objects.requireNonNull(mType);
        }

        public long size() {
            return mSize;
        }
    }
}
