package com.aefyr.sai.backup2;

import androidx.annotation.DrawableRes;

import com.aefyr.sai.R;

public enum BackupStatus {
    NO_BACKUP, SAME_VERSION, HIGHER_VERSION, LOWER_VERSION, APP_NOT_INSTALLED;

    public static BackupStatus fromInstalledAppAndBackupVersions(long installedAppVersion, long backupVersion) {
        if (backupVersion == installedAppVersion)
            return BackupStatus.SAME_VERSION;
        else if (backupVersion > installedAppVersion)
            return BackupStatus.HIGHER_VERSION;
        else
            return BackupStatus.LOWER_VERSION;
    }

    @DrawableRes
    public int getIconRes() {
        return switch (this) {
            case NO_BACKUP -> R.drawable.ic_backup_status_no_backup;
            case SAME_VERSION -> R.drawable.ic_backup_status_same_version;
            case HIGHER_VERSION -> R.drawable.ic_backup_status_higher_version;
            case LOWER_VERSION -> R.drawable.ic_backup_status_lower_version;
            case APP_NOT_INSTALLED -> R.drawable.ic_backup_status_not_installed;
        };

    }

    public boolean canBeInstalledOverExistingApp() {
        return switch (this) {
            case SAME_VERSION, HIGHER_VERSION, APP_NOT_INSTALLED -> true;
            case LOWER_VERSION, NO_BACKUP -> false;
        };

    }
}
