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

    @Query("SELECT products.* FROM products INNER JOIN favorites ON products.id = favorites.productId WHERE favorites.userId = :userId ORDER BY products.timestamp DESC")
    androidx.lifecycle.LiveData<java.util.List<ProductEntity>> getFavoriteProducts(String userId);

    // Sửa lại câu Query kiểm tra isFavorite
    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE userId = :userId AND productId = :productId)")
    androidx.lifecycle.LiveData<Boolean> isFavorite(String userId, String productId);

    @Query("DELETE FROM favorites WHERE userId = :userId AND productId = :productId")
    void removeFavorite(String userId, String productId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertFavorites(List<FavoriteEntity> favorites);

    @Query("DELETE FROM favorites")
    void clearFavorites();
}