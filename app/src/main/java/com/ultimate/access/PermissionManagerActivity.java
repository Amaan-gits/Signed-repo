package com.ultimate.access;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManagerActivity extends AppCompatActivity {

    private static final String TAG = "PermManager";

    private Button btnNext;
    private Button btnCheckStatus;
    private TextView tvStatus;
    private TextView tvProgress;

    private List<String> runtimePermissions = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_manager);

        btnNext = findViewById(R.id.btn_next);
        btnCheckStatus = findViewById(R.id.btn_check_status);
        tvStatus = findViewById(R.id.tv_status);
        tvProgress = findViewById(R.id.tv_progress);

        setupPermissions();

        logPermissionStatus();
        logSpecialPermissionStatus();

        btnNext.setOnClickListener(v -> {
            Log.d(TAG, "NEXT button clicked");
            startMainActivity();
        });

        btnCheckStatus.setOnClickListener(v -> {
            Log.d(TAG, "CHECK STATUS clicked");
            showFullPermissionStatus();
        });

        tvStatus.setText("⚠️ Some permissions may be missing");
        tvProgress.setText("Click NEXT to continue");
    }

    private void setupPermissions() {
        // Location
        runtimePermissions.add(Manifest.permission.ACCESS_FINE_LOCATION);
        runtimePermissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);

        // Camera & Audio
        runtimePermissions.add(Manifest.permission.CAMERA);
        runtimePermissions.add(Manifest.permission.RECORD_AUDIO);

        // Contacts & Calls
        runtimePermissions.add(Manifest.permission.READ_CONTACTS);
        runtimePermissions.add(Manifest.permission.READ_CALL_LOG);
        runtimePermissions.add(Manifest.permission.CALL_PHONE);
        runtimePermissions.add(Manifest.permission.READ_PHONE_STATE);

        // SMS
        runtimePermissions.add(Manifest.permission.READ_SMS);
        runtimePermissions.add(Manifest.permission.RECEIVE_SMS);

        // 🔥 WIFI PERMISSIONS ADD KI
        runtimePermissions.add(Manifest.permission.ACCESS_WIFI_STATE);
        runtimePermissions.add(Manifest.permission.ACCESS_NETWORK_STATE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            runtimePermissions.add(Manifest.permission.READ_MEDIA_IMAGES);
            runtimePermissions.add(Manifest.permission.READ_MEDIA_VIDEO);
            runtimePermissions.add(Manifest.permission.READ_MEDIA_AUDIO);
            runtimePermissions.add(Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    private void logPermissionStatus() {
        Log.d(TAG, "=== RUNTIME PERMISSIONS STATUS ===");

        for (String perm : runtimePermissions) {
            int status = ContextCompat.checkSelfPermission(this, perm);
            Log.d(TAG, perm + " : " + (status == PackageManager.PERMISSION_GRANTED ? "GRANTED" : "DENIED"));
        }
    }

    private void logSpecialPermissionStatus() {
        Log.d(TAG, "=== SPECIAL PERMISSIONS STATUS ===");

        Log.d(TAG, "Usage Stats: " + (hasUsageStatsPermission() ? "GRANTED" : "DENIED"));
        Log.d(TAG, "Overlay: " + (Settings.canDrawOverlays(this) ? "GRANTED" : "DENIED"));
        Log.d(TAG, "Battery Optimization: " + (isIgnoringBatteryOptimizations() ? "GRANTED" : "DENIED"));
        Log.d(TAG, "Notification Listener: " + (isNotificationListenerEnabled() ? "GRANTED" : "DENIED"));
    }

    private void showFullPermissionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("📋 PERMISSION STATUS\n\n");
        status.append("Runtime Permissions\n");

        for (String perm : runtimePermissions) {
            int res = ContextCompat.checkSelfPermission(this, perm);
            String name = perm.substring(perm.lastIndexOf('.') + 1);
            status.append(name).append(" : ").append(res == PackageManager.PERMISSION_GRANTED ? "✅" : "❌").append("\n");
        }

        status.append("\nSpecial Permissions\n");
        status.append("Usage Stats : ").append(hasUsageStatsPermission() ? "✅" : "❌").append("\n");
        status.append("Overlay : ").append(Settings.canDrawOverlays(this) ? "✅" : "❌").append("\n");
        status.append("Battery Optimization : ").append(isIgnoringBatteryOptimizations() ? "✅" : "❌").append("\n");
        status.append("Notification Listener : ").append(isNotificationListenerEnabled() ? "✅" : "❌").append("\n");

        new AlertDialog.Builder(this)
                .setTitle("Permission Status")
                .setMessage(status.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private boolean hasUsageStatsPermission() {
        try {
            android.app.AppOpsManager appOps = (android.app.AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(
                    android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(),
                    getPackageName());
            return mode == android.app.AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(
                getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }

    private boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void startMainActivity() {
        Log.d(TAG, "Opening MainActivity");
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("from_permission", true);
        startActivity(intent);
        finish();
    }
}