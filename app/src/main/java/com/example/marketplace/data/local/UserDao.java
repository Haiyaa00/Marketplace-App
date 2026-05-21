package com.example.marketplace.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertUser(UserEntity user);

    // Trả về LiveData để Profile UI tự động update khi info thay đổi
    @Query("SELECT * FROM users LIMIT 1")
    LiveData<UserEntity> getCurrentUser();

    @Query("DELETE FROM users")
    void clearUser();
}
