package com.aefyr.sai.utils;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

/**
 * Manages delaying notification to avoid going over notifications per second limit
 */
public class NotificationHelper {
    private static final long NOTIFICATION_CD = 1000 / 4;

    private static NotificationHelper sInstance;

    private NotificationManagerCompat mNotificationManager;
    private Context mApplicationContext;
    private Handler mHandler = new Handler(Looper.getMainLooper());

    private long mLastNotificationTime = 0;

    public static NotificationHelper getInstance(Context context) {
        synchronized (NotificationHelper.class) {
            return sInstance != null ? sInstance : new NotificationHelper(context);
        }
    }

    private NotificationHelper(Context c) {
        mApplicationContext = c.getApplicationContext();
        mNotificationManager = NotificationManagerCompat.from(mApplicationContext);
        sInstance = this;
    }

    /**
     * Check if notification permission is granted
     */
    private boolean hasNotificationPermission() {
        return ContextCompat.checkSelfPermission(mApplicationContext,
                android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Post notification when possible or skip it, if it is skipable
     *
     * @param id           id of the notification
     * @param notification notification to post
     * @param skipable     if notification can be skipped (such as progress notifications)
     */
    public void notify(int id, Notification notification, boolean skipable) {
        notify(null, id, notification, skipable);
    }

    /**
     * Post notification when possible or skip it, if it is skipable
     *
     * @param tag          tag on the notification
     * @param id           id of the notification
     * @param notification notification to post
     * @param skipable     if notification can be skipped (such as progress notifications)
     */
    public synchronized void notify(@Nullable String tag, int id, Notification notification, boolean skipable) {
        if (!hasNotificationPermission()) {
            return;
        }

        long timeSinceLastNotification = SystemClock.uptimeMillis() - mLastNotificationTime;

        if (timeSinceLastNotification < NOTIFICATION_CD) {
            if (!skipable) {
                mHandler.postAtTime(() -> {
                    if (hasNotificationPermission()) {
                        mNotificationManager.notify(tag, id, notification);
                    }
                }, mLastNotificationTime + NOTIFICATION_CD);
                mLastNotificationTime = mLastNotificationTime + NOTIFICATION_CD;
            }
            return;
        }

        mLastNotificationTime = SystemClock.uptimeMillis();
        mNotificationManager.notify(tag, id, notification);
    }

    public void cancel(@Nullable String tag, int id) {
        mNotificationManager.cancel(tag, id);
    }

    public void cancel(int id) {
        cancel(null, id);
    }
}