package com.ultimate.access.collectors;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.util.Log;

import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.ultimate.access.network.ApiClient;
import com.ultimate.access.network.ApiService;

import java.util.HashMap;
import java.util.Map;

public class LocationCollector {

    private static final String TAG = "LocationCollector";
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private Location lastLocation;

    public LocationCollector(Context context) {
        this.context = context;
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
    }

    public void startCollection() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permission not granted");
            return;
        }

        // 🔥 HIGH ACCURACY LOCATION REQUEST
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(5000)                  // 5 seconds
                .setFastestInterval(2000)            // 2 seconds
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setSmallestDisplacement(0);          // 0 meter movement pe bhi update

        // 🔥 For Android 12+ (API 31+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            locationRequest.setPriority(Priority.PRIORITY_HIGH_ACCURACY);
        }

        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult != null) {
                    for (Location location : locationResult.getLocations()) {
                        if (location != null) {
                            // 🔥 SIRF ACCURATE LOCATION BHEJO (accuracy < 50m)
                            if (location.hasAccuracy() && location.getAccuracy() < 50) {
                                lastLocation = location;
                                sendLocationToServer(location);
                                Log.d(TAG, "📍 Location: " + location.getLatitude() + ", " + location.getLongitude() +
                                        " | Accuracy: " + location.getAccuracy() + "m");
                            } else {
                                Log.d(TAG, "⚠️ Low accuracy location ignored: " +
                                        (location.hasAccuracy() ? location.getAccuracy() + "m" : "No accuracy"));
                            }
                        }
                    }
                }
            }
        };

        try {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
            Log.d(TAG, "✅ Location collection started");
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception: " + e.getMessage());
        }
    }

    private void sendLocationToServer(Location location) {
        String deviceId = android.provider.Settings.Secure.getString(
                context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        Map<String, Object> locationData = new HashMap<>();
        locationData.put("deviceId", deviceId);
        locationData.put("latitude", location.getLatitude());
        locationData.put("longitude", location.getLongitude());
        locationData.put("accuracy", location.getAccuracy());
        locationData.put("speed", location.getSpeed());
        locationData.put("bearing", location.getBearing());
        locationData.put("altitude", location.getAltitude());
        locationData.put("provider", location.getProvider());
        locationData.put("timestamp", System.currentTimeMillis());

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.sendLocation(locationData).enqueue(new retrofit2.Callback<ApiService.ServerResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.ServerResponse> call,
                                   retrofit2.Response<ApiService.ServerResponse> response) {
                if (response.isSuccessful()) {
                    Log.d(TAG, "✅ Location sent to server");
                } else {
                    Log.e(TAG, "❌ Location send failed: " + response.code());
                }
            }

            @Override
            public void onFailure(retrofit2.Call<ApiService.ServerResponse> call, Throwable t) {
                Log.e(TAG, "❌ Location send error: " + t.getMessage());
            }
        });
    }

    public void stopCollection() {
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "🛑 Location collection stopped");
        }
    }

    public Location getLastLocation() {
        return lastLocation;
    }
}