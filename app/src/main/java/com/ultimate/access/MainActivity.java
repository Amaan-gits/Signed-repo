package com.ultimate.access;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

// 🔥 IMPORT ADD KIYA
import com.ultimate.access.network.DataSyncService;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private TextView tvStatus, tvDeviceId;
    private Button btnServerSync, btnExit;
    private Handler handler = new Handler();
    private boolean isInitialized = false;
    private static boolean isRedirecting = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(TAG, "✅ MainActivity started");

        if (savedInstanceState != null) {
            Log.d(TAG, "Activity recreating, restoring state");
        }

        setContentView(R.layout.activity_main);
        Log.d(TAG, "✅ Layout set successfully");

        // Initialize views
        tvStatus = findViewById(R.id.tv_status);
        tvDeviceId = findViewById(R.id.tv_device_id);
        btnServerSync = findViewById(R.id.btn_server_sync);
        btnExit = findViewById(R.id.btn_exit);

        if (tvStatus == null) {
            Log.e(TAG, "❌ tv_status not found in layout!");
            Toast.makeText(this, "Layout error: tv_status missing", Toast.LENGTH_LONG).show();
            return;
        }

        if (tvDeviceId == null) {
            Log.e(TAG, "❌ tv_device_id not found in layout!");
            Toast.makeText(this, "Layout error: tv_device_id missing", Toast.LENGTH_LONG).show();
            return;
        }

        Log.d(TAG, "✅ All views initialized successfully");

        // Check if we came from PermissionManager
        boolean fromPermissionManager = getIntent().getBooleanExtra("from_permission", false);
        Log.d(TAG, "From PermissionManager: " + fromPermissionManager);

        // Check permissions
        boolean permissionsGranted = UltimateApplication.areAllPermissionsGranted();
        Log.d(TAG, "Permissions granted: " + permissionsGranted);

        if (!permissionsGranted && !isRedirecting && !fromPermissionManager) {
            Log.d(TAG, "➡️ Permissions not granted, redirecting to PermissionManager");
            isRedirecting = true;
            Intent intent = new Intent(this, PermissionManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("from_main", true);
            startActivity(intent);
            finish();
            return;
        }

        if (permissionsGranted) {
            startDataSyncService();
            initViews();
            isInitialized = true;
        } else {
            tvStatus.setText("⚠️ Permissions required. Please grant permissions.");
        }
    }

    private void startDataSyncService() {
        Log.d(TAG, "Starting DataSyncService");
        // 🔥 FIX: Direct class name use karo
        Intent serviceIntent = new Intent(this, DataSyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void initViews() {
        if (isInitialized) {
            Log.d(TAG, "Already initialized, skipping");
            return;
        }

        Log.d(TAG, "initViews() called");

        try {
            String androidId = android.provider.Settings.Secure.getString(
                    getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            tvDeviceId.setText("Device ID: " + androidId);
            Log.d(TAG, "Device ID: " + androidId);

            tvStatus.setText("✅ All Permissions Granted\nApp is running in background");
            Log.d(TAG, "Status text set");

            btnServerSync.setOnClickListener(v -> {
                Log.d(TAG, "Server Sync button clicked");
                Intent intent = new Intent(MainActivity.this, ServerSyncActivity.class);
                startActivity(intent);
            });

            btnExit.setOnClickListener(v -> {
                Log.d(TAG, "Exit button clicked");
                finishAffinity();
            });

            Toast.makeText(this, "Ultimate Access is running", Toast.LENGTH_LONG).show();
            Log.d(TAG, "Toast shown");

            isInitialized = true;

        } catch (Exception e) {
            Log.e(TAG, "Error in initViews: " + e.getMessage());
            e.printStackTrace();
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume() called");

        isRedirecting = false;

        boolean permissionsGranted = UltimateApplication.areAllPermissionsGranted();
        Log.d(TAG, "Permission check in onResume: " + permissionsGranted);

        if (!permissionsGranted) {
            tvStatus.setText("⚠️ Permissions required. Please grant permissions.");
        } else if (permissionsGranted && !isInitialized) {
            initViews();
            startDataSyncService();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause() called");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy() called");
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    public void onPermissionsGranted() {
        Log.d(TAG, "✅ onPermissionsGranted() called from UltimateApplication");
        runOnUiThread(() -> {
            if (!isInitialized) {
                initViews();
                startDataSyncService();
            }
        });
    }
}