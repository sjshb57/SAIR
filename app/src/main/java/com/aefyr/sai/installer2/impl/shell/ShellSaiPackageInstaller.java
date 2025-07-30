package com.aefyr.sai.installer2.impl.shell;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Pair;

import com.aefyr.sai.R;
import com.aefyr.sai.installer2.base.model.AndroidPackageInstallerError;
import com.aefyr.sai.installer2.base.model.SaiPiSessionParams;
import com.aefyr.sai.installer2.base.model.SaiPiSessionState;
import com.aefyr.sai.installer2.base.model.SaiPiSessionStatus;
import com.aefyr.sai.installer2.impl.BaseSaiPackageInstaller;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.shell.Shell;
import com.aefyr.sai.utils.DbgPreferencesHelper;
import com.aefyr.sai.utils.Logs;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ShellSaiPackageInstaller extends BaseSaiPackageInstaller {

    private final Semaphore mSharedSemaphore = new Semaphore(1);
    private final AtomicBoolean mAwaitingBroadcast = new AtomicBoolean(false);
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);
    private final HandlerThread mWorkerThread = new HandlerThread("RootlessSaiPi Worker");
    private final Handler mWorkerHandler;

    private String mCurrentSessionId;

    private final BroadcastReceiver mPackageInstalledBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(tag(), intent.toString());

            if (!mAwaitingBroadcast.get())
                return;

            mAwaitingBroadcast.set(false);

            String installedPackage;
            try {
                String dataString = intent.getDataString();
                installedPackage = dataString != null ? dataString.replace("package:", "") : "";
                String installerPackage = getContext().getPackageManager().getInstallerPackageName(installedPackage);
                Log.d(tag(), "installerPackage=" + installerPackage);
                if (!context.getPackageName().equals(installerPackage))
                    return;
            } catch (Exception e) {
                Log.wtf(tag(), e);
                return;
            }

            setSessionState(mCurrentSessionId, new SaiPiSessionState.Builder(mCurrentSessionId, SaiPiSessionStatus.INSTALLATION_SUCCEED)
                    .packageName(installedPackage)
                    .resolvePackageMeta(getContext())
                    .build());
            unlockInstallation();
        }
    };

    protected ShellSaiPackageInstaller(Context c) {
        super(c);

        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        IntentFilter packageAddedFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        packageAddedFilter.addDataScheme("package");
        getContext().registerReceiver(mPackageInstalledBroadcastReceiver, packageAddedFilter, null, mWorkerHandler);
    }

    @Override
    public void enqueueSession(String sessionId) {
        SaiPiSessionParams params = takeCreatedSession(sessionId);
        setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.QUEUED)
                .appTempName(params.apkSource().getAppName())
                .build());
        mExecutor.submit(() -> install(sessionId, params));
    }

    private void install(String sessionId, SaiPiSessionParams params) {
        lockInstallation(sessionId);
        String appTempName = params.apkSource().getAppName();
        setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLING)
                .appTempName(appTempName)
                .build());

        Integer androidSessionId = null;
        try (ApkSource apkSource = params.apkSource()) {
            if (!getShell().isAvailable()) {
                setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED)
                        .error(getContext().getString(R.string.installer_error_shell, getInstallerName(), getShellUnavailableMessage()), null)
                        .build());
                unlockInstallation();
                return;
            }

            androidSessionId = createSession();

            int currentApkFile = 0;
            while (apkSource.nextApk()) {
                if (apkSource.getApkLength() == -1) {
                    setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED)
                            .appTempName(appTempName)
                            .error(getContext().getString(R.string.installer_error_unknown_apk_size), null)
                            .build());
                    unlockInstallation();
                    return;
                }
                ensureCommandSucceeded(getShell().exec(new Shell.Command("pm", "install-write", "-S",
                        String.valueOf(apkSource.getApkLength()), String.valueOf(androidSessionId),
                        String.format(Locale.US, "%d.apk", currentApkFile++)), apkSource.openApkInputStream()));
            }

            mAwaitingBroadcast.set(true);
            Shell.Result installationResult = getShell().exec(new Shell.Command("pm", "install-commit", String.valueOf(androidSessionId)));
            if (!installationResult.isSuccessful()) {
                mAwaitingBroadcast.set(false);

                String shortError = getContext().getString(R.string.installer_error_shell, getInstallerName(),
                        getSessionInfo(apkSource) + "\n\n" + parseError(installationResult));
                setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED)
                        .appTempName(appTempName)
                        .error(shortError, shortError + "\n\n" + installationResult.out)
                        .build());

                unlockInstallation();
            }
        } catch (Exception e) {
            Log.w(tag(), e);

            if (androidSessionId != null) {
                getShell().exec(new Shell.Command("pm", "install-abandon", String.valueOf(androidSessionId)));
            }

            setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED)
                    .appTempName(appTempName)
                    .error(getContext().getString(R.string.installer_error_shell, getInstallerName(),
                                    getSessionInfo(params.apkSource()) + "\n\n" + e.getLocalizedMessage()),
                            getContext().getString(R.string.installer_error_shell, getInstallerName(),
                                    getSessionInfo(params.apkSource()) + "\n\n" + Utils.throwableToString(e)))
                    .build());

            unlockInstallation();
        }
    }

    private void lockInstallation(String sessionId) {
        try {
            mSharedSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new RuntimeException("wtf", e);
        }
        mCurrentSessionId = sessionId;
    }

    private void unlockInstallation() {
        mSharedSemaphore.release();
    }

    private void ensureCommandSucceeded(Shell.Result result) {
        if (!result.isSuccessful())
            throw new RuntimeException(result.out);
    }

    private String getSessionInfo(ApkSource apkSource) {
        String saiVersion = "???";
        try {
            saiVersion = getContext().getPackageManager()
                    .getPackageInfo(getContext().getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.wtf(tag(), "Unable to get SAI version", e);
        }
        return String.format(Locale.US, "%s: %s %s | %s | Android %s | Using %s ApkSource implementation | SAI %s",
                getContext().getString(R.string.installer_device), Build.BRAND, Build.MODEL,
                Build.VERSION.RELEASE, apkSource.getClass().getSimpleName(), saiVersion);
    }

    private int createSession() {
        String installLocation = String.valueOf(PreferencesHelper.getInstance(getContext()).getInstallLocation());
        ArrayList<Shell.Command> commandsToAttempt = new ArrayList<>();

        String customInstallCreateCommand = DbgPreferencesHelper.getInstance(getContext()).getCustomInstallCreateCommand();
        if (customInstallCreateCommand != null) {
            ArrayList<String> args = new ArrayList<>(Arrays.asList(customInstallCreateCommand.split(" ")));
            String command = args.remove(0);
            commandsToAttempt.add(new Shell.Command(command, args.toArray(new String[0])));
            Logs.d(tag(), "Using custom install-create command: " + customInstallCreateCommand);
        } else {
            commandsToAttempt.add(new Shell.Command("pm", "install-create", "-r", "--install-location",
                    installLocation, "-i", getShell().makeLiteral(getContext().getPackageName())));
            commandsToAttempt.add(new Shell.Command("pm", "install-create", "-r", "-i",
                    getShell().makeLiteral(getContext().getPackageName())));
        }

        List<Pair<Shell.Command, String>> attemptedCommands = new ArrayList<>();

        for (Shell.Command commandToAttempt : commandsToAttempt) {
            Shell.Result result = getShell().exec(commandToAttempt);
            attemptedCommands.add(new Pair<>(commandToAttempt, result.out));

            if (!result.isSuccessful()) {
                Log.w(tag(), String.format(Locale.US, "Command failed: %s > %s", commandToAttempt, result.out));
                continue;
            }

            Integer sessionId = extractSessionId(result.out);
            if (sessionId != null)
                return sessionId;
            else
                Log.w(tag(), String.format(Locale.US, "Command failed: %s > %s", commandToAttempt, result.out));
        }

        StringBuilder exceptionMessage = new StringBuilder("Unable to create session, attempted commands: ");
        int i = 1;
        for (Pair<Shell.Command, String> attemptedCommand : attemptedCommands) {
            exceptionMessage.append("\n\n").append(i++).append(") ==========================\n")
                    .append(attemptedCommand.first)
                    .append("\nVVVVVVVVVVVVVVVV\n")
                    .append(attemptedCommand.second);
        }
        exceptionMessage.append("\n");

        throw new IllegalStateException(exceptionMessage.toString());
    }

    private Integer extractSessionId(String commandResult) {
        try {
            Pattern sessionIdPattern = Pattern.compile("(\\d+)");
            Matcher sessionIdMatcher = sessionIdPattern.matcher(commandResult);
            if (sessionIdMatcher.find()) {
                String group = sessionIdMatcher.group(1);
                return group != null ? Integer.parseInt(group) : null;
            }
            return null;
        } catch (Exception e) {
            Log.w(tag(), commandResult, e);
            return null;
        }
    }

    private String parseError(Shell.Result installCommitResult) {
        AndroidPackageInstallerError matchedError = AndroidPackageInstallerError.UNKNOWN;
        for (AndroidPackageInstallerError error : AndroidPackageInstallerError.values()) {
            if (installCommitResult.out.contains(error.getError())) {
                matchedError = error;
                break;
            }
        }

        return matchedError.getDescription(getContext());
    }

    protected abstract Shell getShell();

    protected abstract String getInstallerName();

    protected abstract String getShellUnavailableMessage();

    protected abstract String tag();
}