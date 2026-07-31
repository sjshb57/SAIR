package com.aefyr.sai.installer2.impl.rootless;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.util.Log;

import androidx.annotation.Nullable;

import com.aefyr.sai.installer2.base.model.SaiPiSessionParams;
import com.aefyr.sai.installer2.base.model.SaiPiSessionState;
import com.aefyr.sai.installer2.base.model.SaiPiSessionStatus;
import com.aefyr.sai.installer2.impl.BaseSaiPackageInstaller;
import com.aefyr.sai.model.apksource.ApkSource;
import com.aefyr.sai.utils.IOUtils;
import com.aefyr.sai.utils.PreferencesHelper;
import com.aefyr.sai.utils.Utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import androidx.core.content.ContextCompat;

public class RootlessSaiPackageInstaller extends BaseSaiPackageInstaller implements RootlessSaiPiBroadcastReceiver.EventObserver {
    private static final String TAG = "RootlessSaiPi";

    private static RootlessSaiPackageInstaller sInstance;

    private final PackageInstaller mPackageInstaller;
    private final ExecutorService mExecutor = Executors.newFixedThreadPool(4);

    private final ConcurrentHashMap<Integer, String> mAndroidPiSessionIdToSaiPiSessionId = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> mSessionIdToAppTempName = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> mSessionIdToCommitStartedAt = new ConcurrentHashMap<>();

    public static RootlessSaiPackageInstaller getInstance(Context c) {
        synchronized (RootlessSaiPackageInstaller.class) {
            return sInstance != null ? sInstance : new RootlessSaiPackageInstaller(c);
        }
    }

    private RootlessSaiPackageInstaller(Context c) {
        super(c);
        mPackageInstaller = getContext().getPackageManager().getPackageInstaller();

        HandlerThread mWorkerThread = new HandlerThread("RootlessSaiPi Worker");
        mWorkerThread.start();
        Handler mWorkerHandler = new Handler(mWorkerThread.getLooper());

        RootlessSaiPiBroadcastReceiver mBroadcastReceiver = new RootlessSaiPiBroadcastReceiver(getContext());
        mBroadcastReceiver.addEventObserver(this);

        ContextCompat.registerReceiver(getContext(), mBroadcastReceiver,
                new IntentFilter(RootlessSaiPiBroadcastReceiver.ACTION_DELIVER_PI_EVENT),
                null, mWorkerHandler, ContextCompat.RECEIVER_NOT_EXPORTED);

        sInstance = this;
    }

    @Override
    public void enqueueSession(String sessionId) {
        SaiPiSessionParams params = takeCreatedSession(sessionId);
        setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.QUEUED).appTempName(params.apkSource().getAppName()).build());
        mExecutor.submit(() -> runInstallation(sessionId, params));
    }

    /**
     * Anything escaping here would be swallowed by the executor and leave the session stuck on
     * INSTALLING with nothing in the log.
     */
    private void runInstallation(String sessionId, SaiPiSessionParams params) {
        try {
            install(sessionId, params);
        } catch (Throwable t) {
            Log.e(TAG, "Installation task crashed", t);
            setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED)
                    .error(t.getLocalizedMessage(), Utils.throwableToString(t))
                    .build());
        }
    }

    private void install(String sessionId, SaiPiSessionParams params) {
        PackageInstaller.Session session = null;
        String appTempName = null;
        try (ApkSource apkSource = params.apkSource()) {
            appTempName = apkSource.getAppName();
            if (appTempName != null)
                mSessionIdToAppTempName.put(sessionId, appTempName);

            setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLING).appTempName(appTempName).build());

            PackageInstaller.SessionParams sessionParams = new PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL);
            sessionParams.setInstallLocation(PreferencesHelper.getInstance(getContext()).getInstallLocation());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                sessionParams.setInstallReason(PackageManager.INSTALL_REASON_USER);

            int androidSessionId = mPackageInstaller.createSession(sessionParams);
            mAndroidPiSessionIdToSaiPiSessionId.put(androidSessionId, sessionId);

            session = mPackageInstaller.openSession(androidSessionId);
            long writeStartedAt = SystemClock.elapsedRealtime();
            long bytesWritten = 0;
            int currentApkFile = 0;
            while (apkSource.nextApk()) {
                OutputStream sessionStream = session.openWrite(String.format(Locale.US, "%d.apk", currentApkFile++), 0, apkSource.getApkLength());
                try (InputStream inputStream = apkSource.openApkInputStream();
                     OutputStream outputStream = IOUtils.buffer(sessionStream)) {
                    bytesWritten += IOUtils.copyStream(inputStream, outputStream);
                    outputStream.flush();
                    session.fsync(sessionStream);
                }
            }

            long writeMs = SystemClock.elapsedRealtime() - writeStartedAt;
            Log.i(TAG, String.format(Locale.US, "Wrote %d bytes in %d ms (%.1f MB/s); handing off to system",
                    bytesWritten, writeMs, writeMs > 0 ? bytesWritten / 1048.576f / writeMs : 0f));
            mSessionIdToCommitStartedAt.put(sessionId, SystemClock.elapsedRealtime());

            Intent callbackIntent = new Intent(RootlessSaiPiBroadcastReceiver.ACTION_DELIVER_PI_EVENT);
            callbackIntent.setPackage(getContext().getPackageName());

            int flags = PendingIntent.FLAG_UPDATE_CURRENT;
            if (Build.VERSION.SDK_INT >= 31) {
                flags |= PendingIntent.FLAG_IMMUTABLE;
            }
            PendingIntent pendingIntent = PendingIntent.getBroadcast(getContext(), 0, callbackIntent, flags);
            session.commit(pendingIntent.getIntentSender());
        } catch (Exception e) {
            Log.w(TAG, e);
            if (session != null)
                session.abandon();

            mSessionIdToAppTempName.remove(sessionId);
            mSessionIdToCommitStartedAt.remove(sessionId);
            mAndroidPiSessionIdToSaiPiSessionId.values().remove(sessionId);

            setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED).appTempName(appTempName).error(e.getLocalizedMessage(), Utils.throwableToString(e)).build());
        } finally {
            if (session != null)
                session.close();
        }
    }

    @Override
    public void onInstallationSucceeded(int androidSessionId, String packageName) {
        String sessionId = mAndroidPiSessionIdToSaiPiSessionId.remove(androidSessionId);
        if (sessionId == null)
            return;

        mSessionIdToAppTempName.remove(sessionId);
        logSystemPhaseDuration(sessionId);
        setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_SUCCEED).packageName(packageName).resolvePackageMeta(getContext()).build());
    }

    @Override
    public void onInstallationFailed(int androidSessionId, String shortError, @Nullable String fullError, @Nullable Exception exception) {
        String sessionId = mAndroidPiSessionIdToSaiPiSessionId.remove(androidSessionId);
        if (sessionId == null)
            return;

        mSessionIdToCommitStartedAt.remove(sessionId);

        setSessionState(sessionId, new SaiPiSessionState.Builder(sessionId, SaiPiSessionStatus.INSTALLATION_FAILED)
                .appTempName(mSessionIdToAppTempName.remove(sessionId))
                .error(shortError, fullError)
                .build());

    }

    private void logSystemPhaseDuration(String sessionId) {
        Long commitStartedAt = mSessionIdToCommitStartedAt.remove(sessionId);
        if (commitStartedAt != null) {
            Log.i(TAG, String.format(Locale.US, "System-side install phase took %d ms (%s)",
                    SystemClock.elapsedRealtime() - commitStartedAt, "succeeded"));
        }
    }

    @Override
    protected String tag() {
        return TAG;
    }
}
