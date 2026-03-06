package com.ultimate.access.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.ultimate.access.R;

public class UltimateBackgroundService extends Service {

    private static final String TAG = "UltimateBgService";
    private static final String CHANNEL_ID = "UltimateChannel";
    private static final int NOTIFICATION_ID = 1001;

    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();

        Log.d(TAG, "UltimateBackgroundService started");

        createNotificationChannel();

        Notification notification = createNotification();

        try {
            startForeground(NOTIFICATION_ID, notification);
        } catch (Exception e) {
            Log.e(TAG, "Foreground start error: " + e.getMessage());
        }

        acquireWakeLock();
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel =
                    new NotificationChannel(
                            CHANNEL_ID,
                            "Ultimate Background Service",
                            NotificationManager.IMPORTANCE_LOW
                    );

            channel.setDescription("Background service running");

            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification createNotification() {

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Ultimate Access")
                .setContentText("Background service running")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void acquireWakeLock() {

        try {

            PowerManager powerManager =
                    (PowerManager) getSystemService(Context.POWER_SERVICE);

            if (powerManager != null) {

                wakeLock = powerManager.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK,
                        "UltimateAccess::WakeLock"
                );

                if (!wakeLock.isHeld()) {
                    wakeLock.acquire();   // safer for Android 13+
                }
            }

        } catch (Exception e) {

            Log.e(TAG, "WakeLock error: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "Service onStartCommand");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        try {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        } catch (Exception e) {
            Log.e(TAG, "WakeLock release error: " + e.getMessage());
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}