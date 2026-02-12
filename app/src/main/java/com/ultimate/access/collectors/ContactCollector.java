package com.ultimate.access.collectors;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract;

import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ContactCollector {

    private Context context;

    public ContactCollector(Context context) {
        this.context = context;
    }

    public List<Map<String, Object>> getContacts() {
        List<Map<String, Object>> contactsList = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
                != PackageManager.PERMISSION_GRANTED) {
            return contactsList;
        }

        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(
                ContactsContract.Contacts.CONTENT_URI,
                null,
                null,
                null,
                ContactsContract.Contacts.DISPLAY_NAME + " ASC"
        );

        if (cursor != null) {
            int idIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
            int nameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);

            while (cursor.moveToNext()) {
                String contactId = cursor.getString(idIndex);
                String displayName = cursor.getString(nameIndex);

                Map<String, Object> contact = new HashMap<>();
                contact.put("name", displayName != null ? displayName : "");

                // Get phone numbers
                List<String> numbers = new ArrayList<>();
                Cursor phoneCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                );

                if (phoneCursor != null) {
                    int numberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                    while (phoneCursor.moveToNext()) {
                        if (numberIndex != -1) {
                            numbers.add(phoneCursor.getString(numberIndex));
                        }
                    }
                    phoneCursor.close();
                }

                contact.put("numbers", numbers);
                if (!numbers.isEmpty()) {
                    contact.put("number", numbers.get(0));
                } else {
                    contact.put("number", "");
                }

                // Get emails
                List<String> emails = new ArrayList<>();
                Cursor emailCursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Email.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = ?",
                        new String[]{contactId},
                        null
                );

                if (emailCursor != null) {
                    int emailIndex = emailCursor.getColumnIndex(ContactsContract.CommonDataKinds.Email.ADDRESS);
                    while (emailCursor.moveToNext()) {
                        if (emailIndex != -1) {
                            emails.add(emailCursor.getString(emailIndex));
                        }
                    }
                    emailCursor.close();
                }

                contact.put("emails", emails);
                if (!emails.isEmpty()) {
                    contact.put("email", emails.get(0));
                } else {
                    contact.put("email", "");
                }

                contactsList.add(contact);
            }
            cursor.close();
        }

        return contactsList;
    }
}