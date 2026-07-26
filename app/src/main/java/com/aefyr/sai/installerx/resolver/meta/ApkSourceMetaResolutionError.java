package com.aefyr.sai.installerx.resolver.meta;

public class ApkSourceMetaResolutionError {


    final String mMessage;
    final boolean mDoesTryingToInstallNonethelessMakeSense;

    public ApkSourceMetaResolutionError(String message, boolean doesTryingToInstallNonethelessMakeSense) {
        mMessage = message;
        mDoesTryingToInstallNonethelessMakeSense = doesTryingToInstallNonethelessMakeSense;
    }

    public String message() {
        return mMessage;
    }

    public boolean doesTryingToInstallNonethelessMakeSense() {
        return mDoesTryingToInstallNonethelessMakeSense;
    }


}
