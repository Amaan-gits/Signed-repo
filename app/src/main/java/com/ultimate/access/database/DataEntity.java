package com.ultimate.access.database;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "data_table")
public class DataEntity {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String type;
    private String data;
    private long timestamp;
    private boolean synced;

    public DataEntity(String type, String data) {
        this.type = type;
        this.data = data;
        this.timestamp = System.currentTimeMillis();
        this.synced = false;
    }

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isSynced() {
        return synced;
    }

    public void setSynced(boolean synced) {
        this.synced = synced;
    }

    public java.util.Date getTimestampAsDate() {
        return new java.util.Date(timestamp);
    }
}