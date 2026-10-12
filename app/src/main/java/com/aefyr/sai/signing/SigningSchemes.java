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
    public static final int SCHEME_V4 = 1 << 3;

    public static final int DEFAULT_SCHEMES = SCHEME_V1 | SCHEME_V2 | SCHEME_V3;

    /** v3 was added in Android 9; older devices fall back to v2 or v1. */
    private static final int MIN_SDK_FOR_V3 = 28;

    /** v4 was added in Android 11. */
    private static final int MIN_SDK_FOR_V4 = 30;

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
        if (scheme == SCHEME_V3)
            return Build.VERSION.SDK_INT >= MIN_SDK_FOR_V3;
        if (scheme == SCHEME_V4)
            return Build.VERSION.SDK_INT >= MIN_SDK_FOR_V4;
        return true;
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

        // Below Android 9 a v3 signature is ignored. Android 7 and 8 OEM ROMs also often
        // require a v1 JAR signature, so ensure both v1 and v2 are available.
        if (Build.VERSION.SDK_INT < MIN_SDK_FOR_V3) {
            if ((flags & (SCHEME_V1 | SCHEME_V2)) == 0) {
                flags |= (SCHEME_V1 | SCHEME_V2);
            } else if ((flags & SCHEME_V1) == 0) {
                flags |= SCHEME_V1;
            }
        }

        // Android 11 and above refuse to install an APK targeting SDK 30+ that only carries v1.
        if ((flags & (SCHEME_V2 | SCHEME_V3)) == 0)
            flags |= SCHEME_V2;

        // Below Android 11 a v4 signature is not supported by the system.
        if (Build.VERSION.SDK_INT < MIN_SDK_FOR_V4)
            flags &= ~SCHEME_V4;

        // v4 signing requires at least v2 or v3 signing to be enabled.
        if ((flags & SCHEME_V4) != 0 && (flags & (SCHEME_V2 | SCHEME_V3)) == 0)
            flags |= SCHEME_V2;

        return new SigningSchemes(flags);
    }
}
