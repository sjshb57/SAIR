package com.aefyr.sai.backup2.impl.components;

import com.aefyr.sai.backup2.BackupComponent;

public class SimpleBackupComponent implements BackupComponent {

    private final String mType;
    private final long mSize;

    public SimpleBackupComponent(String type, long size) {
        mType = type;
        mSize = size;
    }

    @Override
    public String type() {
        return mType;
    }

    @Override
    public long size() {
        return mSize;
    }
}
