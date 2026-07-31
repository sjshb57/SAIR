package com.aefyr.sai.model.apksource;

import android.content.Context;

import androidx.annotation.Nullable;

import com.aefyr.sai.signing.SaiApkSigner;
import com.aefyr.sai.signing.SigningKeyManager;
import com.aefyr.sai.signing.SigningSchemes;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.PreferencesHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Re-signs every APK of the wrapped source. apksig needs random access to both sides, so each APK is
 * staged to a file first.
 */
public class SignerApkSource implements ApkSource {

    private final ApkSource mWrappedApkSource;
    private final Context mContext;

    private SaiApkSigner mSigner;
    private SigningSchemes mSchemes;
    private File mTempDir;

    private File mCurrentSignedApkFile;
    private int mApkIndex;

    public SignerApkSource(Context c, ApkSource apkSource) {
        mContext = c.getApplicationContext();
        mWrappedApkSource = apkSource;
    }

    @Override
    public boolean nextApk() throws Exception {
        if (!mWrappedApkSource.nextApk())
            return false;

        if (mSigner == null) {
            mSigner = new SaiApkSigner(SigningKeyManager.getInstance().getOrCreate());
            mSchemes = PreferencesHelper.getInstance(mContext).getSigningSchemes();
            createTempDir();
        }

        // A previous APK is dropped before the next one is written, so two entries sharing a name
        // cannot make the new file delete itself.
        deleteCurrentSignedApk();

        int index = mApkIndex++;
        File unsigned = new File(mTempDir, index + "-unsigned.apk");
        try (InputStream in = mWrappedApkSource.openApkInputStream();
             OutputStream out = IOUtils.buffer(new FileOutputStream(unsigned))) {
            IOUtils.copyStream(in, out);
        }

        File signed = new File(mTempDir, index + "-signed.apk");
        try {
            mSigner.sign(unsigned, signed, mSchemes);
        } finally {
            //noinspection ResultOfMethodCallIgnored
            unsigned.delete();
        }

        mCurrentSignedApkFile = signed;
        return true;
    }

    @Override
    public InputStream openApkInputStream() throws Exception {
        return IOUtils.buffer(new FileInputStream(mCurrentSignedApkFile));
    }

    @Override
    public long getApkLength() {
        return mCurrentSignedApkFile.length();
    }

    @Override
    public String getApkName() throws Exception {
        return mWrappedApkSource.getApkName();
    }

    @Override
    public String getApkLocalPath() throws Exception {
        return mWrappedApkSource.getApkLocalPath();
    }

    @Nullable
    @Override
    public String getAppName() {
        return mWrappedApkSource.getAppName();
    }

    @Override
    public void close() throws Exception {
        if (mTempDir != null)
            IOUtils.deleteRecursively(mTempDir);

        mWrappedApkSource.close();
    }

    private void deleteCurrentSignedApk() {
        if (mCurrentSignedApkFile != null) {
            //noinspection ResultOfMethodCallIgnored
            mCurrentSignedApkFile.delete();
            mCurrentSignedApkFile = null;
        }
    }

    private void createTempDir() throws IOException {
        mTempDir = new File(mContext.getCacheDir(), "SignerApkSource-" + System.nanoTime());
        if (!mTempDir.mkdirs() && !mTempDir.isDirectory())
            throw new IOException("Unable to create a staging directory for signing");
    }
}
