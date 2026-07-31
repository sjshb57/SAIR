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
            signWith(input, output, resolved, Build.VERSION.SDK_INT);
        }

        return resolved;
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

        ApkSigner.Builder builder = new ApkSigner.Builder(Collections.singletonList(signerConfig))
                .setInputApk(input)
                .setOutputApk(output)
                .setV1SigningEnabled(schemes.has(SigningSchemes.SCHEME_V1))
                .setV2SigningEnabled(schemes.has(SigningSchemes.SCHEME_V2))
                .setV3SigningEnabled(schemes.has(SigningSchemes.SCHEME_V3))
                .setV4SigningEnabled(false)
                .setAlignmentPreserved(PRESERVE_ALIGNMENT);

        if (minSdkVersion != null)
            builder.setMinSdkVersion(minSdkVersion);

        builder.build().sign();
    }
}
