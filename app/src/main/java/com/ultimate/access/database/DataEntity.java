package com.ultimate.access.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "data_table")
public class DataEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String type;
    private String data;

    // 🔴 CHANGED: Date → long (Room database ke liye)
    private long timestamp;  // ✅ Ab ye milliseconds me store hoga

    private boolean synced;

    // 🔴 CHANGED: Constructor - ab Date ki jagah System.currentTimeMillis()
    public DataEntity(String type, String data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();  // ✅ Current time in milliseconds
        this.synced = false;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    // 🔴 CHANGED: Return long instead of Date
    public long getTimestamp() {
        return timestamp;
    }

    // 🔴 CHANGED: Accept long instead of Date
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    // ✅ OPTIONAL: Helper method agar Date object chahiye to
    public java.util.Date getTimestampAsDate() {
        return new java.util.Date(timestamp);
    }
}