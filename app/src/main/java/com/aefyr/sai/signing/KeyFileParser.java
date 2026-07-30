package com.aefyr.sai.signing;

import android.util.Base64;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.Cipher;
import javax.crypto.EncryptedPrivateKeyInfo;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Reads the loose key and certificate files apksigner and the AOSP build system use
 * (key.pk8 + cert.x509.pem), in either DER or PEM form.
 */
public final class KeyFileParser {

    private static final Pattern PEM_BLOCK =
            Pattern.compile("-----BEGIN ([A-Z0-9 ]+)-----(.*?)-----END \\1-----", Pattern.DOTALL);

    /** Algorithms to try, since a PKCS#8 blob does not say which one it holds in a way we can read. */
    private static final String[] KEY_ALGORITHMS = {"RSA", "EC", "DSA"};

    private KeyFileParser() {
    }

    @NonNull
    public static X509Certificate parseCertificate(@NonNull byte[] bytes) throws Exception {
        // CertificateFactory understands both PEM and DER on its own.
        return (X509Certificate) CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(bytes));
    }

    @NonNull
    public static PrivateKey parsePrivateKey(@NonNull byte[] bytes, @Nullable char[] password) throws Exception {
        PemBlock pem = readPemBlock(bytes);
        byte[] der = pem != null ? pem.data : bytes;

        if (pem != null && pem.type.contains("RSA PRIVATE KEY")) {
            throw new IllegalArgumentException("This is a PKCS#1 key. Convert it first: "
                    + "openssl pkcs8 -topk8 -in key.pem -out key.pk8 -outform DER -nocrypt");
        }

        boolean encrypted = pem != null
                ? pem.type.contains("ENCRYPTED")
                : looksEncrypted(der);

        PKCS8EncodedKeySpec keySpec = encrypted
                ? decrypt(der, password)
                : new PKCS8EncodedKeySpec(der);

        Exception lastError = null;
        for (String algorithm : KEY_ALGORITHMS) {
            try {
                return KeyFactory.getInstance(algorithm).generatePrivate(keySpec);
            } catch (Exception e) {
                lastError = e;
            }
        }

        throw new IllegalArgumentException("Unsupported private key format", lastError);
    }

    private static PKCS8EncodedKeySpec decrypt(byte[] der, @Nullable char[] password) throws Exception {
        if (password == null || password.length == 0)
            throw new IllegalArgumentException("The private key is encrypted, a password is required");

        EncryptedPrivateKeyInfo info = new EncryptedPrivateKeyInfo(der);
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance(info.getAlgName());

        Cipher cipher = Cipher.getInstance(info.getAlgName());
        cipher.init(Cipher.DECRYPT_MODE,
                secretKeyFactory.generateSecret(new PBEKeySpec(password)),
                info.getAlgParameters());

        return info.getKeySpec(cipher);
    }

    /** An unencrypted PKCS#8 blob starts with a SEQUENCE holding version 0. */
    private static boolean looksEncrypted(byte[] der) {
        try {
            new EncryptedPrivateKeyInfo(der);
            return !(der.length > 4 && der[0] == 0x30 && der[4] == 0x00);
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    private static PemBlock readPemBlock(byte[] bytes) {
        String text;
        try {
            text = new String(bytes, StandardCharsets.US_ASCII);
        } catch (Exception e) {
            return null;
        }

        Matcher matcher = PEM_BLOCK.matcher(text);
        if (!matcher.find())
            return null;

        String type = matcher.group(1);
        String body = matcher.group(2);
        if (type == null || body == null)
            return null;

        return new PemBlock(type, Base64.decode(body.replaceAll("\\s", ""), Base64.DEFAULT));
    }

    private static final class PemBlock {
        final String type;
        final byte[] data;

        PemBlock(String type, byte[] data) {
            this.type = type;
            this.data = data;
        }
    }
}
