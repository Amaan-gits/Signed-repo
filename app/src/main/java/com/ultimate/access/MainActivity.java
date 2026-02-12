package com.ultimate.access;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private TextView tvStatus, tvDeviceId;
    private Button btnServerSync, btnExit;
    private Handler handler = new Handler();
    private Runnable checkPermissionRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvDeviceId = findViewById(R.id.tv_device_id);
        btnServerSync = findViewById(R.id.btn_server_sync);
        btnExit = findViewById(R.id.btn_exit);

        // Check if permissions are granted
        if (!UltimateApplication.areAllPermissionsGranted()) {
            Intent intent = new Intent(this, PermissionManagerActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

        initViews();
    }

    private void initViews() {
        String androidId = android.provider.Settings.Secure.getString(
                getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        tvDeviceId.setText("Device ID: " + androidId);
        tvStatus.setText("✅ All Permissions Granted\nApp is running in background");

        btnServerSync.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ServerSyncActivity.class);
            startActivity(intent);
        });

        btnExit.setOnClickListener(v -> {
            finishAffinity();
        });

        Toast.makeText(this, "Ultimate Access is running", Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkPermissionRunnable = () -> {
            if (!UltimateApplication.areAllPermissionsGranted()) {
                Intent intent = new Intent(MainActivity.this, PermissionManagerActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            handler.postDelayed(checkPermissionRunnable, 2000);
        };
        handler.post(checkPermissionRunnable);
    }

    @Override
    protected void onPause() {
        super.onPause();
        handler.removeCallbacks(checkPermissionRunnable);
    }
}