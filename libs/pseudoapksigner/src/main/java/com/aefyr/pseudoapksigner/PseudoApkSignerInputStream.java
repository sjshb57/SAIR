package com.aefyr.pseudoapksigner;

import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

@SuppressWarnings("unused")
public class PseudoApkSignerInputStream extends InputStream {
    private PseudoApkSigner mPseudoApkSigner;

    private PipedInputStream mPipeInput;
    private PipedOutputStream mPipeOutput;

    /**
     * Creates a new PseudoApkSignerInputStream that wraps apk InputStream and signs it with given template and private key
     *
     * @param inputStream apk file input stream to wrap, you will be able to read signed version of the apk from this PseudoApkSignerInputStream
     * @param template    .RSA file from an APK signed with an actual apksigner with the same private key with last 256 bytes removed
     * @param privateKey  .pk8 private key file
     */
    public PseudoApkSignerInputStream(File template, File privateKey, final InputStream inputStream) throws Exception {
        init(template, privateKey, null, inputStream);
    }

    /**
     * Creates a new PseudoApkSignerInputStream that wraps apk InputStream and signs it with given template, private key and signer name
     *
     * @param inputStream apk file input stream to wrap, you will be able to read signed version of the apk from this PseudoApkSignerInputStream
     * @param template    .RSA file from an APK signed with an actual apksigner with the same private key with last 256 bytes removed
     * @param signerName  desired .SF and .RSA files name
     * @param privateKey  .pk8 private key file
     */
    public PseudoApkSignerInputStream(File template, File privateKey, String signerName, final InputStream inputStream) throws Exception {
        init(template, privateKey, signerName, inputStream);
    }


    private void init(File template, File privateKey, String signerName, final InputStream inputStream) throws Exception {
        mPseudoApkSigner = new PseudoApkSigner(template, privateKey);
        if (signerName != null)
            mPseudoApkSigner.setSignerName(signerName);

        mPipeOutput = new PipedOutputStream();
        mPipeInput = new PipedInputStream(1024 * 1024);
        mPipeInput.connect(mPipeOutput);

        new Thread(() -> {
            try {
                mPseudoApkSigner.sign(inputStream, mPipeOutput);
            } catch (Exception e) {
                Log.w("PASInputStream", e);
            }
        }).start();
    }

    @Override
    public int read() throws IOException {
        return mPipeInput.read();
    }

    @Override
    public int read(byte[] b) throws IOException {
        return mPipeInput.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return mPipeInput.read(b, off, len);
    }

    @Override
    public int available() throws IOException {
        return mPipeInput.available();
    }

    @Override
    public void close() throws IOException {
        mPipeInput.close();
    }
}
