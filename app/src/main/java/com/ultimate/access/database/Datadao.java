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

    @Query("SELECT * FROM data_table ORDER BY timestamp DESC")
    List<DataEntity> getAllData();

    @Query("SELECT * FROM data_table WHERE type = :type ORDER BY timestamp DESC")
    List<DataEntity> getDataByType(String type);

    @Query("SELECT * FROM data_table WHERE timestamp BETWEEN :startTime AND :endTime")
    List<DataEntity> getDataBetween(long startTime, long endTime);

    @Query("DELETE FROM data_table WHERE timestamp < :cutoffTime")
    void deleteOldData(long cutoffTime);

    @Query("DELETE FROM data_table")
    void deleteAll();

    @Query("SELECT COUNT(*) FROM data_table")
    int getCount();
}