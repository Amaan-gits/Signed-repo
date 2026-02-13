package com.ultimate.access.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DataDao {

    @Insert
    void insert(DataEntity data);

    @Update
    void update(DataEntity data);

    @Delete
    void delete(DataEntity data);

    // ✅ @Query - SAHI HAI!
    @Query("SELECT * FROM data_table ORDER BY timestamp DESC")
    List<DataEntity> getAllData();

    @Query("SELECT * FROM data_table WHERE type = :type ORDER BY timestamp DESC")
    List<DataEntity> getDataByType(String type);

    // 🔴 IMPORTANT: ab timestamp long hai, isliye cutoffTime bhi long hona chahiye
    @Query("DELETE FROM data_table WHERE timestamp < :cutoffTime")
    void deleteOldData(long cutoffTime);  // ✅ ye sahi hai

    @Query("DELETE FROM data_table")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM data_table")
    int getCount();
}