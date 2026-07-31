package com.aefyr.sai.model.apksource;

import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.model.filedescriptor.FileDescriptor;

import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;

/**
 * Reads a zip by streaming it, and falls back to the random access reader when the archive turns out
 * to be one ZipInputStream cannot handle.
 * <p>
 * The fallback only happens before the first APK has been handed out. Once the installer has written
 * an APK into a session, starting over would write it twice, so a later failure is passed on.
 */
public class FallbackZipApkSource implements ZipBackedApkSource {

    private static final String TAG = "FallbackZipApkSource";

    private final Context mContext;
    private final FileDescriptor mFileDescriptor;

    private ZipBackedApkSource mDelegate;
    private boolean mSwitched;
    private int mApksReturned;

    public FallbackZipApkSource(Context context, FileDescriptor fileDescriptor) {
        mContext = context.getApplicationContext();
        mFileDescriptor = fileDescriptor;
    }

    @Override
    public boolean nextApk() throws Exception {
        if (mDelegate == null)
            mDelegate = new ZipApkSource(mContext, mFileDescriptor);

        try {
            return count(mDelegate.nextApk());
        } catch (ZipException e) {
            if (mSwitched || mApksReturned > 0)
                throw e;

            Log.i(TAG, "Streaming read failed, retrying with the ZipFile reader", e);

            mSwitched = true;
            closeDelegate();
            mDelegate = new ZipFileApkSource(mContext, mFileDescriptor);

            return count(mDelegate.nextApk());
        }
    }

    private boolean count(boolean hasApk) {
        if (hasApk)
            mApksReturned++;

        return hasApk;
    }

    @Override
    public InputStream openApkInputStream() throws Exception {
        return mDelegate.openApkInputStream();
    }

    @Override
    public long getApkLength() throws Exception {
        return mDelegate.getApkLength();
    }

    @Override
    public String getApkName() throws Exception {
        return mDelegate.getApkName();
    }

    @Override
    public String getApkLocalPath() throws Exception {
        return mDelegate.getApkLocalPath();
    }

    @Nullable
    @Override
    public String getAppName() {
        return mDelegate != null ? mDelegate.getAppName() : null;
    }

    @Override
    public ZipEntry getEntry() {
        return mDelegate.getEntry();
    }

    @Override
    public void close() throws Exception {
        closeDelegate();
    }

    private void closeDelegate() {
        if (mDelegate == null)
            return;

        try {
            mDelegate.close();
        } catch (Exception e) {
            Log.w(TAG, "Unable to close the previous reader", e);
        } finally {
            mDelegate = null;
        }
    }
}
