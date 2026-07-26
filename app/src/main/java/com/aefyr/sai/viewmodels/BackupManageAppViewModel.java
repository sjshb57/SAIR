package com.aefyr.sai.viewmodels;

import android.content.Context;

import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import com.aefyr.sai.backup2.Backup;
import com.aefyr.sai.backup2.BackupAppDetails;
import com.aefyr.sai.backup2.BackupManager;
import com.aefyr.sai.backup2.impl.DefaultBackupManager;

public class BackupManageAppViewModel extends ViewModel {

    private final String mPackage;

    private final LiveData<BackupAppDetails> mDetailsLiveData;

    public BackupManageAppViewModel(Context appContext, String pkg) {
        mPackage = pkg;

        BackupManager mBackupManager = DefaultBackupManager.getInstance(appContext);

        mDetailsLiveData = mBackupManager.getAppDetails(pkg);
    }

    public LiveData<BackupAppDetails> getDetails() {
        return mDetailsLiveData;
    }

    public String getPackage() {
        return mPackage;
    }

    @Nullable
    public Backup getLatestBackup() {
        BackupAppDetails details = mDetailsLiveData.getValue();
        if (details == null)
            return null;

        if (!details.backups().isEmpty())
            return details.backups().get(0);

        return null;
    }
}
