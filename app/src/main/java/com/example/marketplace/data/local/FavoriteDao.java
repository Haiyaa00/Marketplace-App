package com.example.marketplace.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface FavoriteDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorite(FavoriteEntity favorite);

    @Query("DELETE FROM favorites WHERE productId = :productId")
    void removeFavorite(String productId);

    @Query("SELECT * FROM favorites")
    LiveData<List<FavoriteEntity>> getAllFavorites();

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE productId = :productId)")
    LiveData<Boolean> isFavorite(String productId);
}
