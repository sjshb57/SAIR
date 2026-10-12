package com.aefyr.sai.signing;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.android.apksig.ApkSigner;
import com.android.apksig.KeyConfig;
import com.android.apksig.apk.MinSdkVersionException;

import java.io.File;
import java.util.Collections;

public class SaiApkSigner {

    private static final String TAG = "SaiApkSigner";

    /**
     * Signing rewrites the whole zip, so alignment of uncompressed entries has to be carried over
     * explicitly. Losing it stops the system from mapping native libraries directly, which matters
     * on the 16 KB page size devices Android 15 introduced.
     */
    private static final boolean PRESERVE_ALIGNMENT = true;

    private final SigningKey mKey;

    public SaiApkSigner(@NonNull SigningKey key) {
        mKey = key;
    }

    /** @return the schemes that were actually written */
    @NonNull
    public SigningSchemes sign(@NonNull File input, @NonNull File output,
                               @NonNull SigningSchemes schemes) throws Exception {
        SigningSchemes resolved = schemes.resolveForThisDevice();

        try {
            signWith(input, output, resolved, null);
        } catch (MinSdkVersionException e) {
            // apksig could not read minSdkVersion out of the manifest, and it needs one to pick
            // digest algorithms. The APK is being installed onto this device, so this device's API
            // level is the one that has to verify it.
            Log.i(TAG, "Unable to read the APK's minSdkVersion, using this device's", e);
            try {
                signWith(input, output, resolved, Build.VERSION.SDK_INT);
            } catch (Exception ex) {
                if (isSha1IncompatibleException(ex) && resolved.has(SigningSchemes.SCHEME_V1)
                        && (resolved.has(SigningSchemes.SCHEME_V2) || resolved.has(SigningSchemes.SCHEME_V3) || resolved.has(SigningSchemes.SCHEME_V4))) {
                    Log.w(TAG, "Key does not support SHA-1 digest required for APK Signature Scheme v1, retrying without v1", ex);
                    SigningSchemes withoutV1 = resolved.with(SigningSchemes.SCHEME_V1, false);
                    signWith(input, output, withoutV1, Build.VERSION.SDK_INT);
                    return withoutV1;
                }
                throw ex;
            }
        } catch (Exception e) {
            if (isSha1IncompatibleException(e) && resolved.has(SigningSchemes.SCHEME_V1)
                    && (resolved.has(SigningSchemes.SCHEME_V2) || resolved.has(SigningSchemes.SCHEME_V3) || resolved.has(SigningSchemes.SCHEME_V4))) {
                Log.w(TAG, "Key does not support SHA-1 digest required for APK Signature Scheme v1, retrying without v1", e);
                SigningSchemes withoutV1 = resolved.with(SigningSchemes.SCHEME_V1, false);
                signWith(input, output, withoutV1, null);
                return withoutV1;
            }
            throw e;
        }

        return resolved;
    }

    private static boolean isSha1IncompatibleException(Throwable t) {
        while (t != null) {
            String msg = t.getMessage();
            if (msg != null && (msg.contains("SHA1withRSA") || msg.contains("INCOMPATIBLE_DIGEST") || msg.contains("Incompatible digest"))) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private void signWith(File input, File output, SigningSchemes schemes,
                          @Nullable Integer minSdkVersion) throws Exception {
        // The (String, PrivateKey, List) constructor is deprecated in favour of the KeyConfig one,
        // which also covers keys held by a KMS. The trailing flag is deterministic DSA signing,
        // which does not apply to an RSA key.
        ApkSigner.SignerConfig signerConfig = new ApkSigner.SignerConfig.Builder(
                "SAI",
                new KeyConfig.Jca(mKey.privateKey()),
                Collections.singletonList(mKey.certificate()),
                false).build();

        boolean v4Enabled = schemes.has(SigningSchemes.SCHEME_V4);
        ApkSigner.Builder builder = new ApkSigner.Builder(Collections.singletonList(signerConfig))
                .setInputApk(input)
                .setOutputApk(output)
                .setV1SigningEnabled(schemes.has(SigningSchemes.SCHEME_V1))
                .setV2SigningEnabled(schemes.has(SigningSchemes.SCHEME_V2))
                .setV3SigningEnabled(schemes.has(SigningSchemes.SCHEME_V3))
                .setV4SigningEnabled(v4Enabled)
                .setAlignmentPreserved(PRESERVE_ALIGNMENT);

        if (v4Enabled) {
            builder.setV4SignatureOutputFile(new File(output.getAbsolutePath() + ".idsig"));
        }

        if (minSdkVersion != null)
            builder.setMinSdkVersion(minSdkVersion);

        builder.build().sign();
    }
}
