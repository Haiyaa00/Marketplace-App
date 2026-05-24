package com.example.marketplace.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {UserEntity.class, ProductEntity.class, FavoriteEntity.class}, version = 1, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();
    public abstract ProductDao productDao();
    public abstract FavoriteDao favoriteDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "marketplace_db")
                            .fallbackToDestructiveMigration() // Xóa data cũ nếu update version db
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
