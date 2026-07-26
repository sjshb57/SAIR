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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import androidx.core.content.ContextCompat;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import com.aefyr.sai.utils.IOUtils;

public abstract class ShellSaiPackageInstaller extends BaseSaiPackageInstaller {

    private final Semaphore mSharedSemaphore = new Semaphore(1);
    private final AtomicBoolean mAwaitingBroadcast = new AtomicBoolean(false);
    private final AtomicReference<String> mBroadcastPackageName = new AtomicReference<>();
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);
    private final HandlerThread mWorkerThread = new HandlerThread("RootlessSaiPi Worker");
    private final Handler mWorkerHandler;

    /**
     * Best-effort source for the installed package name. Success is decided by the exit code of
     * pm install-commit, never by this broadcast.
     */
    private final BroadcastReceiver mPackageInstalledBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (!mAwaitingBroadcast.get())
                return;

            String dataString = intent.getDataString();
            if (dataString == null)
                return;

            String installedPackage = dataString.replace("package:", "");
            try {
                String installerPackage = getInstallerPackage(getContext(), installedPackage);
                if (!context.getPackageName().equals(installerPackage))
                    return;
            } catch (Exception e) {
                // Package visibility may hide the installer info; keep the name anyway.
                Log.d(tag(), "Unable to verify installer package for " + installedPackage, e);
            }

            mBroadcastPackageName.set(installedPackage);
        }
    };

    protected ShellSaiPackageInstaller(Context c) {
        super(c);

        mWorkerThread.start();
        mWorkerHandler = new Handler(mWorkerThread.getLooper());

        IntentFilter packageAddedFilter = new IntentFilter(Intent.ACTION_PACKAGE_ADDED);
        packageAddedFilter.addDataScheme("package");
        ContextCompat.registerReceiver(getContext(), mPackageInstalledBroadcastReceiver, packageAddedFilter,
                null, mWorkerHandler, ContextCompat.RECEIVER_NOT_EXPORTED);
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
        lockInstallation();
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
                String splitName = String.format(Locale.US, "%d.apk", currentApkFile++);
                long apkLength = apkSource.getApkLength();

                if (apkLength == -1) {
                    // Streamed zip entries carry no size in their local header. Materialise the
                    // split into cache so pm gets a definite -S value instead of failing outright.
                    File stagedApk = stageApkToCache(apkSource);
                    try {
                        ensureCommandSucceeded(getShell().exec(new Shell.Command("pm", "install-write", "-S",
                                String.valueOf(stagedApk.length()), String.valueOf(androidSessionId), splitName),
                                IOUtils.buffer(new FileInputStream(stagedApk))));
                    } finally {
                        //noinspection ResultOfMethodCallIgnored
                        stagedApk.delete();
                    }
                } else {
                    ensureCommandSucceeded(getShell().exec(new Shell.Command("pm", "install-write", "-S",
                            String.valueOf(apkLength), String.valueOf(androidSessionId), splitName),
                            apkSource.openApkInputStream()));
                }
            }

            mAwaitingBroadcast.set(true);
            Shell.Result installationResult = getShell().exec(new Shell.Command("pm", "install-commit", String.valueOf(androidSessionId)));
            mAwaitingBroadcast.set(false);

            if (!installationResult.isSuccessful()) {
                String shortError = getContext().getString(R.string.installer_error_shell, getInstallerName(),
                        getSessionInfo(apkSource) + "\n\n" + parseError(installationResult));
                setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED)
                        .appTempName(appTempName)
                        .error(shortError, shortError + "\n\n" + installationResult.out)
                        .build());
                unlockInstallation();
                return;
            }

            // install-commit 已同步确认成功,不再等 PACKAGE_ADDED 广播:
            // Android 11+ 的包可见性过滤会让广播丢失或包名查询失败,
            // 旧实现在那种情况下永远不会 unlockInstallation,导致后续安装全部死锁。
            String installedPackage = mBroadcastPackageName.getAndSet(null);
            SaiPiSessionState.Builder success = new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_SUCCEED)
                    .appTempName(appTempName);
            if (installedPackage != null)
                success.packageName(installedPackage).resolvePackageMeta(getContext());
            setSessionState(sessionId, success.build());
            unlockInstallation();
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

    private void lockInstallation() {
        try {
            mSharedSemaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for the installation lock", e);
        }
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
                Build.DEVICE, Build.VERSION.RELEASE, apkSource.getClass().getSimpleName(), saiVersion);
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

    @SuppressWarnings("deprecation")
    private static String getInstallerPackage(Context context, String packageName) throws Exception {
        PackageManager pm = context.getPackageManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
            return pm.getInstallSourceInfo(packageName).getInstallingPackageName();
        return pm.getInstallerPackageName(packageName);
    }

    private File stageApkToCache(ApkSource apkSource) throws Exception {
        File staged = Utils.createTempFileInCache(getContext(), "ShellSaiPi", "apk");
        try (InputStream in = apkSource.openApkInputStream();
             OutputStream out = IOUtils.buffer(new FileOutputStream(staged))) {
            IOUtils.copyStream(in, out);
        }
        return staged;
    }
}