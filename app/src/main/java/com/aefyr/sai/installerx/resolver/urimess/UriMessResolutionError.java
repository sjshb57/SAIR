package com.aefyr.sai.installerx.resolver.urimess;

public class UriMessResolutionError {

    final String mMessage;
    final boolean mDoesTryingToInstallNonethelessMakeSense;

    public UriMessResolutionError(String message, boolean doesTryingToInstallNonethelessMakeSense) {
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
