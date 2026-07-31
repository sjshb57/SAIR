package com.aefyr.sai.signing;

import android.content.Context;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.KeyProtection;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.security.auth.x500.X500Principal;

/**
 * Holds the key APK signing uses. Everything lives in AndroidKeyStore, which generates the
 * self-signed certificate for us - Android has no public API for building one, and the only
 * alternative is pulling in Bouncy Castle for over a megabyte.
 * <p>
 * The trade-off is that a generated key cannot be exported. Anyone who needs a portable key creates
 * it with keytool and imports the PKCS#12 file instead.
 */
public class SigningKeyManager {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "sai_apk_signing_key";
    private static final int KEY_SIZE = 2048;
    private static final int VALIDITY_YEARS = 30;

    /** Tried in order; PKCS#12 is guaranteed, the rest depend on the device's providers. */
    private static final String[] KEYSTORE_TYPES = {"PKCS12", "BKS", "BouncyCastle", "JKS"};

    private static SigningKeyManager sInstance;

    private final Context mContext;

    public static SigningKeyManager getInstance(Context context) {
        synchronized (SigningKeyManager.class) {
            if (sInstance == null)
                sInstance = new SigningKeyManager(context.getApplicationContext());

            return sInstance;
        }
    }

    private SigningKeyManager(Context context) {
        mContext = context;
    }

    @NonNull
    public synchronized SigningKey getOrCreate() throws Exception {
        SigningKey existing = get();
        if (existing != null)
            return existing;

        generate();

        SigningKey created = get();
        if (created == null)
            throw new IllegalStateException("Key generation reported success but nothing was stored");

        return created;
    }

    @Nullable
    public synchronized SigningKey get() throws Exception {
        KeyStore keyStore = loadAndroidKeyStore();
        if (!keyStore.containsAlias(KEY_ALIAS))
            return null;

        PrivateKey privateKey = (PrivateKey) keyStore.getKey(KEY_ALIAS, null);
        Certificate certificate = keyStore.getCertificate(KEY_ALIAS);

        if (privateKey == null || !(certificate instanceof X509Certificate))
            return null;

        return new SigningKey(privateKey, (X509Certificate) certificate);
    }

    public synchronized void generate() throws Exception {
        delete();

        Calendar calendar = Calendar.getInstance();
        Date notBefore = calendar.getTime();
        calendar.add(Calendar.YEAR, VALIDITY_YEARS);
        Date notAfter = calendar.getTime();

        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_SIGN)
                .setKeySize(KEY_SIZE)
                .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                .setCertificateSubject(new X500Principal("CN=SAI"))
                .setCertificateSerialNumber(new BigInteger(64, new SecureRandom()).abs())
                .setCertificateNotBefore(notBefore)
                .setCertificateNotAfter(notAfter)
                .build();

        KeyPairGenerator generator =
                KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
        generator.initialize(spec);
        generator.generateKeyPair();
    }

    /**
     * Works out what the picked files actually are instead of making the user say. One file is a
     * keystore, or an archive holding a key and a certificate; two files are a key and a
     * certificate in either order.
     */
    public synchronized void importFrom(@NonNull List<byte[]> files, @NonNull char[] password) throws Exception {
        if (files.isEmpty())
            throw new IllegalArgumentException("No file selected");

        if (files.size() == 2) {
            importPair(files.get(0), files.get(1), password);
            return;
        }

        if (files.size() > 2)
            throw new IllegalArgumentException("Select either a keystore, or a key and a certificate");

        byte[] file = files.get(0);
        Exception keyStoreError;
        try {
            importKeyStore(file, password);
            return;
        } catch (Exception e) {
            keyStoreError = e;
        }

        List<byte[]> unpacked = unpackArchive(file);
        if (unpacked.size() == 2) {
            importPair(unpacked.get(0), unpacked.get(1), password);
            return;
        }

        throw new IllegalArgumentException("Could not read this as a keystore, and it holds no key and "
                + "certificate pair either. Check the password if the file is a keystore.", keyStoreError);
    }

    /** Whichever of the two parses as a certificate is the certificate; the other is the key. */
    private void importPair(byte[] first, byte[] second, char[] password) throws Exception {
        if (isCertificate(first)) {
            importKeyAndCertificate(second, first, password);
            return;
        }

        if (isCertificate(second)) {
            importKeyAndCertificate(first, second, password);
            return;
        }

        throw new IllegalArgumentException("Neither file is an X.509 certificate");
    }

    /**
     * Pulls the first key and certificate out of a zip, so a bundle of the two can be picked in one
     * go rather than in two rounds of the file picker.
     */
    private static List<byte[]> unpackArchive(byte[] archive) {
        List<byte[]> candidates = new ArrayList<>();

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.isDirectory())
                    continue;

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = zip.read(buffer)) != -1)
                    out.write(buffer, 0, read);

                candidates.add(out.toByteArray());
            }
        } catch (Exception e) {
            return Collections.emptyList();
        }

        byte[] certificate = null;
        byte[] key = null;
        for (byte[] candidate : candidates) {
            if (certificate == null && isCertificate(candidate))
                certificate = candidate;
            else if (key == null)
                key = candidate;
        }

        return certificate != null && key != null ? Arrays.asList(key, certificate) : Collections.emptyList();
    }

    private static boolean isCertificate(byte[] bytes) {
        try {
            KeyFileParser.parseCertificate(bytes);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Imports a keystore file. Which formats work depends on the security providers the device
     * ships: PKCS#12 is always there, BKS usually is, JKS normally is not. Rather than guessing,
     * every type the platform offers is tried until one parses the file.
     */
    private synchronized void importKeyStore(@NonNull byte[] bytes, @NonNull char[] password) throws Exception {
        KeyStore source = null;
        Exception lastError = null;

        for (String type : KEYSTORE_TYPES) {
            try {
                KeyStore candidate = KeyStore.getInstance(type);
                candidate.load(new ByteArrayInputStream(bytes), password);
                source = candidate;
                break;
            } catch (Exception e) {
                lastError = e;
            }
        }

        if (source == null)
            throw new IllegalArgumentException("Unsupported keystore format, or wrong password", lastError);

        String sourceAlias = findKeyAlias(source);
        if (sourceAlias == null)
            throw new IllegalArgumentException("The keystore contains no private key");

        PrivateKey privateKey = (PrivateKey) source.getKey(sourceAlias, password);
        Certificate[] chain = source.getCertificateChain(sourceAlias);

        if (privateKey == null || chain == null || chain.length == 0)
            throw new IllegalArgumentException("The keystore entry has no private key or certificate");

        if (!(chain[0] instanceof X509Certificate))
            throw new IllegalArgumentException("The certificate is not an X.509 certificate");

        store(privateKey, chain);
    }

    /**
     * Imports the loose key and certificate files apksigner takes, e.g. a key.pk8 next to a
     * cert.x509.pem.
     */
    private synchronized void importKeyAndCertificate(@NonNull byte[] keyBytes, @NonNull byte[] certBytes,
                                                     @NonNull char[] password) throws Exception {
        PrivateKey privateKey = KeyFileParser.parsePrivateKey(keyBytes, password);
        X509Certificate certificate = KeyFileParser.parseCertificate(certBytes);

        store(privateKey, new Certificate[]{certificate});
    }

    private void store(PrivateKey privateKey, Certificate[] chain) throws Exception {
        KeyStore keyStore = loadAndroidKeyStore();
        keyStore.deleteEntry(KEY_ALIAS);
        keyStore.setEntry(KEY_ALIAS,
                new KeyStore.PrivateKeyEntry(privateKey, chain),
                new KeyProtection.Builder(KeyProperties.PURPOSE_SIGN)
                        .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
                        .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
                        .build());
    }

    public synchronized void delete() throws Exception {
        loadAndroidKeyStore().deleteEntry(KEY_ALIAS);
    }

    @Nullable
    private static String findKeyAlias(KeyStore keyStore) throws Exception {
        Enumeration<String> aliases = keyStore.aliases();
        while (aliases.hasMoreElements()) {
            String alias = aliases.nextElement();
            if (keyStore.isKeyEntry(alias))
                return alias;
        }
        return null;
    }

    private static KeyStore loadAndroidKeyStore() throws Exception {
        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
        keyStore.load(null);
        return keyStore;
    }
}
