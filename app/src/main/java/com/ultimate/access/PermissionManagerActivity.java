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
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class PermissionManagerActivity extends AppCompatActivity {

    private static final String TAG = "PermManager";
    private static final int PERMISSION_REQUEST_CODE = 9999;
    private static final int OVERLAY_PERMISSION_CODE = 1001;
    private static final int USAGE_ACCESS_CODE = 1002;
    private static final int NOTIFICATION_LISTENER_CODE = 1003;
    private static final int BATTERY_OPTIMIZATION_CODE = 1005;
    private static final int MANAGE_STORAGE_CODE = 1006;

    private Button btnNext;
    private Button btnCheckStatus;
    private TextView tvStatus;
    private TextView tvProgress;

    private boolean isRequestingPermissions = false;

    private String[] runtimePermissions = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.BODY_SENSORS,
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
            Manifest.permission.POST_NOTIFICATIONS
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permission_manager);

        btnNext = findViewById(R.id.btn_next);
        btnCheckStatus = findViewById(R.id.btn_check_status);
        tvStatus = findViewById(R.id.tv_status);
        tvProgress = findViewById(R.id.tv_progress);

        boolean fromMain = getIntent().getBooleanExtra("from_main", false);
        Log.d(TAG, "Launched from MainActivity: " + fromMain);

        logPermissionStatus();
        logSpecialPermissionStatus();

        btnNext.setOnClickListener(v -> {
            if (areAllRuntimePermissionsGranted() && areAllSpecialPermissionsGranted()) {
                startMainActivity();
            } else {
                showDetailedPermissionDialog();
            }
        });

        btnCheckStatus.setOnClickListener(v -> {
            showFullPermissionStatus();
        });

        if (areAllRuntimePermissionsGranted() && areAllSpecialPermissionsGranted()) {
            startMainActivity();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        logSpecialPermissionStatus();
        isRequestingPermissions = false;

        if (areAllRuntimePermissionsGranted() && areAllSpecialPermissionsGranted()) {
            startMainActivity();
        }
    }

    private void checkAndProceed() {
        if (areAllRuntimePermissionsGranted() && areAllSpecialPermissionsGranted()) {
            Log.d(TAG, "✅ All permissions granted! Starting MainActivity");
            startMainActivity();
        } else {
            Log.d(TAG, "❌ Permissions still pending");
        }
    }

    private void logPermissionStatus() {
        Log.d(TAG, "=== RUNTIME PERMISSIONS STATUS ===");
        for (String perm : runtimePermissions) {
            int status = ContextCompat.checkSelfPermission(this, perm);
            Log.d(TAG, perm + " : " + (status == PackageManager.PERMISSION_GRANTED ? "✅" : "❌"));
        }
    }

    private void logSpecialPermissionStatus() {
        Log.d(TAG, "=== SPECIAL PERMISSIONS STATUS ===");
        Log.d(TAG, "Usage Stats: " + (hasUsageStatsPermission() ? "✅" : "❌"));
        Log.d(TAG, "Overlay: " + (Settings.canDrawOverlays(this) ? "✅" : "❌"));
        Log.d(TAG, "Battery Opt: " + (isIgnoringBatteryOptimizations() ? "✅" : "❌"));
        Log.d(TAG, "Notification Listener: " + (isNotificationListenerEnabled() ? "✅" : "❌"));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Log.d(TAG, "Storage Mgmt: " + (android.os.Environment.isExternalStorageManager() ? "✅" : "❌"));
        }
    }

    private void showFullPermissionStatus() {
        StringBuilder status = new StringBuilder();
        status.append("📋 PERMISSION STATUS:\n\n");
        status.append("🔹 RUNTIME PERMISSIONS:\n");
        for (String perm : runtimePermissions) {
            int res = ContextCompat.checkSelfPermission(this, perm);
            String permName = perm.substring(perm.lastIndexOf('.') + 1);
            status.append(permName).append(": ").append(res == PackageManager.PERMISSION_GRANTED ? "✅" : "❌").append("\n");
        }

        status.append("\n🔸 SPECIAL PERMISSIONS:\n");
        status.append("Usage Stats: ").append(hasUsageStatsPermission() ? "✅" : "❌").append("\n");
        status.append("Overlay: ").append(Settings.canDrawOverlays(this) ? "✅" : "❌").append("\n");
        status.append("Battery Opt: ").append(isIgnoringBatteryOptimizations() ? "✅" : "❌").append("\n");
        status.append("Notification Listener: ").append(isNotificationListenerEnabled() ? "✅" : "❌").append("\n");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            status.append("Storage Mgmt: ").append(android.os.Environment.isExternalStorageManager() ? "✅" : "❌").append("\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Permission Status")
                .setMessage(status.toString())
                .setPositiveButton("OK", null)
                .show();
    }

    private void showDetailedPermissionDialog() {
        StringBuilder message = new StringBuilder();
        message.append("❌ Pending Permissions:\n\n");

        for (String perm : runtimePermissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                String permName = perm.substring(perm.lastIndexOf('.') + 1);
                message.append("• ").append(permName).append(" (Runtime)\n");
            }
        }

        if (!hasUsageStatsPermission()) {
            message.append("• Usage Stats (Special)\n");
        }
        if (!Settings.canDrawOverlays(this)) {
            message.append("• Overlay (Special)\n");
        }
        if (!isIgnoringBatteryOptimizations()) {
            message.append("• Battery Optimization (Special)\n");
        }
        if (!isNotificationListenerEnabled()) {
            message.append("• Notification Listener (Special)\n");
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
                !android.os.Environment.isExternalStorageManager()) {
            message.append("• Storage Management (Special)\n");
        }

        new AlertDialog.Builder(this)
                .setTitle("Permissions Required")
                .setMessage(message.toString())
                .setPositiveButton("Grant Now", (dialog, which) -> {
                    requestRuntimePermissions();
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    private void requestRuntimePermissions() {
        if (isRequestingPermissions) {
            Log.d(TAG, "Already requesting permissions, skipping");
            return;
        }

        isRequestingPermissions = true;

        List<String> permissionsToRequest = new ArrayList<>();

        for (String permission : runtimePermissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }

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

        isRequestingPermissions = false;

        if (requestCode == PERMISSION_REQUEST_CODE) {
            boolean allGranted = true;
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    Log.d(TAG, "❌ Permission denied: " + permissions[i]);
                } else {
                    Log.d(TAG, "✅ Permission granted: " + permissions[i]);
                }
            }
            if (allGranted) {
                requestSpecialPermissions();
            } else {
                updateProgressMessage();
            }
        }
    }

    private void requestSpecialPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!android.os.Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, MANAGE_STORAGE_CODE);
                return;
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, OVERLAY_PERMISSION_CODE);
                return;
            }
        }

        if (!hasUsageStatsPermission()) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            startActivityForResult(intent, USAGE_ACCESS_CODE);
            return;
        }

        if (!isNotificationListenerEnabled()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivityForResult(intent, NOTIFICATION_LISTENER_CODE);
            return;
        }

        if (!isIgnoringBatteryOptimizations()) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivityForResult(intent, BATTERY_OPTIMIZATION_CODE);
            return;
        }

        updateProgressMessage();
        checkAndProceed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "onActivityResult: requestCode=" + requestCode);
        requestSpecialPermissions();
        checkAndProceed();
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

    private boolean isIgnoringBatteryOptimizations() {
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }

    private boolean areAllRuntimePermissionsGranted() {
        for (String permission : runtimePermissions) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private boolean areAllSpecialPermissionsGranted() {
        boolean usage = hasUsageStatsPermission();
        boolean overlay = Settings.canDrawOverlays(this);
        boolean battery = isIgnoringBatteryOptimizations();
        boolean storage = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
                android.os.Environment.isExternalStorageManager();
        boolean notif = isNotificationListenerEnabled();

        return usage && overlay && battery && storage && notif;
    }

    private void updateProgressMessage() {
        if (areAllRuntimePermissionsGranted() && areAllSpecialPermissionsGranted()) {
            tvProgress.setText("✅ All permissions granted!");
            tvProgress.setTextColor(getColor(android.R.color.holo_green_dark));
        } else {
            tvProgress.setText("⚠️ Some permissions are still pending");
            tvProgress.setTextColor(getColor(android.R.color.holo_orange_dark));
        }
    }

    // 🔥 FIXED: MainActivity start karne se pehle flag set karo
    private void startMainActivity() {
        Log.d(TAG, "All permissions granted! Setting flag and starting MainActivity");

        // IMPORTANT: Flag set karo
        UltimateApplication.setAllPermissionsGranted(true);

        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        intent.putExtra("from_permission", true);
        startActivity(intent);
        finish();
    }
}