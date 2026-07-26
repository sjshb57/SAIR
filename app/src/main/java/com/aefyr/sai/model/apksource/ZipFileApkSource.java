package com.aefyr.sai.model.apksource;

import android.content.Context;

import androidx.annotation.Nullable;

import com.aefyr.sai.R;
import com.aefyr.sai.model.filedescriptor.FileDescriptor;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.Locale;

/**
 * An ApkSource implementation that copies given zip file FileDescriptor to a temp file and uses {@link ZipFile} API to read APKs from it.
 * Used to read zip archives that are not compatible with ZipInputStream.
 */
public class ZipFileApkSource implements ZipBackedApkSource {

    private final Context mContext;
    private final FileDescriptor mZipFileDescriptor;

    private File mTempFile;
    private ZipFile mZipFile;

    private Enumeration<? extends ZipEntry> mZipEntries;

    private ZipEntry mCurrentEntry;

    private boolean mSeenApkFile;

    public ZipFileApkSource(Context context, FileDescriptor zipFileDescriptor) {
        mContext = context.getApplicationContext();
        mZipFileDescriptor = zipFileDescriptor;
    }

    @Override
    public boolean nextApk() throws Exception {
        if (mZipFile == null)
            copyAndOpenZip();

        mCurrentEntry = null;
        while (mCurrentEntry == null && mZipEntries.hasMoreElements()) {
            ZipEntry nextEntry = mZipEntries.nextElement();
            if (!nextEntry.isDirectory() && nextEntry.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) {
                mCurrentEntry = nextEntry;
                mSeenApkFile = true;
            }
        }

        if (mCurrentEntry == null) {
            if (!mSeenApkFile)
                throw new IllegalArgumentException(mContext.getString(R.string.installer_error_zip_contains_no_apks));

            return false;
        }

        return true;
    }

    private void copyAndOpenZip() throws Exception {
        mTempFile = createTempFile();

        try (InputStream in = mZipFileDescriptor.open();
             OutputStream out = IOUtils.buffer(new FileOutputStream(mTempFile))) {
            IOUtils.copyStream(in, out);
        }

        mZipFile = new ZipFile(mTempFile);
        mZipEntries = mZipFile.entries();
    }

    @Override
    public InputStream openApkInputStream() throws Exception {
        return IOUtils.buffer(mZipFile.getInputStream(mCurrentEntry));
    }

    @Override
    public long getApkLength() {
        return mCurrentEntry.getSize();
    }

    @Override
    public String getApkName() {
        return Utils.getFileNameFromZipEntry(mCurrentEntry);
    }

    @Override
    public String getApkLocalPath() throws Exception {
        return mCurrentEntry.getName();
    }

    @Override
    public void close() throws Exception {
        if (mZipFile != null)
            mZipFile.close();

        if (mTempFile != null)
            IOUtils.deleteRecursively(mTempFile);
    }

    @Nullable
    @Override
    public String getAppName() {
        try {
            return mZipFileDescriptor.name();
        } catch (Exception e) {
            return null;
        }
    }

    private File createTempFile() {
        File tempFile = new File(mContext.getFilesDir(), "ZipFileApkSource");
        //noinspection ResultOfMethodCallIgnored
        tempFile.mkdir();
        tempFile = new File(tempFile, System.currentTimeMillis() + ".zip");
        return tempFile;
    }

    @Override
    public ZipEntry getEntry() {
        return mCurrentEntry;
    }
}
