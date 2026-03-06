package com.ultimate.access.services;

import static android.app.Activity.RESULT_OK;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class ScreenCaptureService extends Service {

    private static final String TAG = "ScreenCapture";
    private static final String CHANNEL_ID = "ScreenCaptureChannel";
    private static final int NOTIFICATION_ID = 1002;

    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private int screenWidth;
    private int screenHeight;
    private int screenDensity;

    @Override
    public void onCreate() {

        super.onCreate();

        createNotificationChannel();

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Screen Capture")
                .setContentText("Screen capture running")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build();

        startForeground(NOTIFICATION_ID, notification);

        backgroundThread = new HandlerThread("ScreenCaptureThread");
        backgroundThread.start();

        backgroundHandler = new Handler(backgroundThread.getLooper());

        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics metrics = new DisplayMetrics();

        wm.getDefaultDisplay().getMetrics(metrics);

        screenWidth = metrics.widthPixels;
        screenHeight = metrics.heightPixels;
        screenDensity = metrics.densityDpi;
    }

    public void startCapture(Intent data, int resultCode) {

        if (data == null || resultCode != RESULT_OK) {
            Log.e(TAG, "Permission denied for screen capture");
            stopSelf();
            return;
        }

        MediaProjectionManager projectionManager =
                (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);

        mediaProjection = projectionManager.getMediaProjection(resultCode, data);

        imageReader = ImageReader.newInstance(
                screenWidth,
                screenHeight,
                PixelFormat.RGBA_8888,
                2
        );

        virtualDisplay = mediaProjection.createVirtualDisplay(
                "ScreenCapture",
                screenWidth,
                screenHeight,
                screenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(),
                null,
                backgroundHandler
        );

        Log.d(TAG, "Screen capture started");
    }

    private void createNotificationChannel() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Screen Capture Service",
                    NotificationManager.IMPORTANCE_LOW
            );

            NotificationManager manager =
                    (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "ScreenCaptureService started");

        return START_STICKY;
    }

    @Override
    public void onDestroy() {

        super.onDestroy();

        if (virtualDisplay != null) {
            virtualDisplay.release();
        }

        if (imageReader != null) {
            imageReader.close();
        }

        if (mediaProjection != null) {
            mediaProjection.stop();
        }

        if (backgroundThread != null) {
            backgroundThread.quitSafely();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}