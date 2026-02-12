package com.ultimate.access;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.ultimate.access.network.ApiClient;
import com.ultimate.access.network.ApiService;

import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ServerSyncActivity extends AppCompatActivity {

    private TextView tvStatus, tvDeviceId, tvLastSync, tvServerUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_sync);

        tvStatus = findViewById(R.id.tv_status);
        tvDeviceId = findViewById(R.id.tv_device_id);
        tvLastSync = findViewById(R.id.tv_last_sync);
        tvServerUrl = findViewById(R.id.tv_server_url);

        String androidId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        tvDeviceId.setText("Device ID: " + androidId);
        tvServerUrl.setText("Server: " + ApiClient.BASE_URL);

        checkServerConnection();
    }

    private void checkServerConnection() {
        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        Call<Map<String, Object>> call = apiService.getHealth();

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    tvStatus.setText("✅ Server Connected");
                    tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                    tvLastSync.setText("Last Sync: " + new java.util.Date().toString());
                } else {
                    tvStatus.setText("❌ Server Error");
                    tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                tvStatus.setText("❌ Cannot Connect to Server");
                tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                tvLastSync.setText("Error: " + t.getMessage());
            }
        });
    }
}