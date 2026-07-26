package com.aefyr.sai.installer2.base.model;

import android.content.Context;

import com.aefyr.sai.R;

public enum SaiPiSessionStatus {
    CREATED, QUEUED, INSTALLING, INSTALLATION_SUCCEED, INSTALLATION_FAILED;

    public String getReadableName(Context c) {
        return switch (this) {
            case CREATED -> c.getString(R.string.installer_state_created);
            case QUEUED -> c.getString(R.string.installer_state_queued);
            case INSTALLING -> c.getString(R.string.installer_state_installing);
            case INSTALLATION_SUCCEED -> c.getString(R.string.installer_state_installed);
            case INSTALLATION_FAILED -> c.getString(R.string.installer_state_failed);
        };

    }
}
