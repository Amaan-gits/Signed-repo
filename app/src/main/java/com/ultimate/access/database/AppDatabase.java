package com.ultimate.access.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;  // ✅ YEH IMPORT ADD KIYA!

@Database(
        entities = {DataEntity.class},
        version = 1,
        exportSchema = false
)
@TypeConverters({Converters.class})  // ✅ YEH LINE ADD KIYA - Date converter ke liye!
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase instance;
    private static final String DATABASE_NAME = "ultimate_access_db";

    public abstract DataDao dataDao();

    public static AppDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (AppDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AppDatabase.class,
                            DATABASE_NAME
                    ).allowMainThreadQueries().build();
                }
            }
        }
        return instance;
    }
}