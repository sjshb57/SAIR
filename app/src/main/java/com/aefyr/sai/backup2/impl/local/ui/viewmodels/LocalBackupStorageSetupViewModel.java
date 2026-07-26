package com.aefyr.sai.backup2.impl.local.ui.viewmodels;

import android.app.Application;
import android.net.Uri;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import com.aefyr.sai.backup2.impl.local.LocalBackupStorageProvider;
import android.os.Looper;

public class LocalBackupStorageSetupViewModel extends AndroidViewModel implements LocalBackupStorageProvider.OnConfigChangeListener {

    private final LocalBackupStorageProvider mProvider;

    private final MutableLiveData<Uri> mBackupDirUriLiveData = new MutableLiveData<>();

    public LocalBackupStorageSetupViewModel(@NonNull Application application) {
        super(application);

        mProvider = LocalBackupStorageProvider.getInstance(getApplication());

        mProvider.addOnConfigChangeListener(this, new Handler(Looper.getMainLooper()));
        mBackupDirUriLiveData.setValue(mProvider.getBackupDirUri());
    }

    public void setBackupDir(Uri backupDirUri) {
        mProvider.setBackupDirUri(backupDirUri);
    }

    @Override
    protected void onCleared() {
        mProvider.removeOnConfigChangeListener(this);
    }

    @Override
    public void onBackupDirChanged() {
        mBackupDirUriLiveData.setValue(mProvider.getBackupDirUri());
    }
}
