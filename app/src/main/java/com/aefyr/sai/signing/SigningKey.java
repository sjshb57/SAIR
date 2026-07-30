package com.aefyr.sai.signing;

import androidx.annotation.NonNull;

import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.Locale;

public final class SigningKey {

    private final PrivateKey mPrivateKey;
    private final X509Certificate mCertificate;

    public SigningKey(@NonNull PrivateKey privateKey, @NonNull X509Certificate certificate) {
        mPrivateKey = privateKey;
        mCertificate = certificate;
    }

    @NonNull
    public PrivateKey privateKey() {
        return mPrivateKey;
    }

    @NonNull
    public X509Certificate certificate() {
        return mCertificate;
    }

    /** The fingerprint users compare against, formatted the way apksigner and keytool print it. */
    @NonNull
    public String certificateSha256() throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(mCertificate.getEncoded());

        StringBuilder builder = new StringBuilder(digest.length * 3);
        for (byte b : digest) {
            if (builder.length() > 0)
                builder.append(':');
            builder.append(String.format(Locale.US, "%02X", b));
        }
        return builder.toString();
    }
}
