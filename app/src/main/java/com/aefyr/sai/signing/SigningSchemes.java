package com.aefyr.sai.signing;

import android.os.Build;

import androidx.annotation.NonNull;

/**
 * Which APK signature schemes to emit, as a bit mask.
 * <p>
 * v1 is the expensive one: it writes a digest line per zip entry into MANIFEST.MF and CERT.SF, which
 * on an APK with many entries costs a noticeable amount of size. It is only actually needed for APKs
 * that support devices older than Android 7.
 */
public final class SigningSchemes {

    public static final int SCHEME_V1 = 1;
    public static final int SCHEME_V2 = 1 << 1;
    public static final int SCHEME_V3 = 1 << 2;

    public static final int DEFAULT_SCHEMES = SCHEME_V2 | SCHEME_V3;

    /** v3 was added in Android 9; older devices fall back to v2 or v1. */
    private static final int MIN_SDK_FOR_V3 = 28;

    private final int mFlags;

    public SigningSchemes(int flags) {
        mFlags = flags;
    }

    public int flags() {
        return mFlags;
    }

    public boolean has(int scheme) {
        return (mFlags & scheme) != 0;
    }

    public boolean isEmpty() {
        return mFlags == 0;
    }

    @NonNull
    public SigningSchemes with(int scheme, boolean enabled) {
        return new SigningSchemes(enabled ? mFlags | scheme : mFlags & ~scheme);
    }

    /**
     * A scheme this device could never verify is pointless to offer, so the UI greys it out.
     */
    public static boolean isSupportedByThisDevice(int scheme) {
        return scheme != SCHEME_V3 || Build.VERSION.SDK_INT >= MIN_SDK_FOR_V3;
    }

    /**
     * SAI always installs onto the device it runs on, so the minimum viable set can be worked out
     * up front rather than guessed. Adds whatever the selection is missing instead of failing.
     */
    @NonNull
    public SigningSchemes resolveForThisDevice() {
        int flags = mFlags;

        // Nothing selected at all would produce an APK no device can verify.
        if (flags == 0)
            flags = DEFAULT_SCHEMES;

        // Below Android 9 a v3 signature is ignored, so something else has to carry the APK.
        if (Build.VERSION.SDK_INT < MIN_SDK_FOR_V3 && (flags & (SCHEME_V1 | SCHEME_V2)) == 0)
            flags |= SCHEME_V2;

        // Android 11 and above refuse to install an APK targeting SDK 30+ that only carries v1.
        if ((flags & (SCHEME_V2 | SCHEME_V3)) == 0)
            flags |= SCHEME_V2;

        return new SigningSchemes(flags);
    }
}
