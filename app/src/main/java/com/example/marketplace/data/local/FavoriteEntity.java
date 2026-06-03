package com.example.marketplace.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

// ĐÃ XÓA KHÓA NGOẠI (FOREIGN KEY) Ở ĐÂY ĐỂ TRÁNH LỖI CASCADE DELETE
@Entity(tableName = "favorites")
public class FavoriteEntity {

    @PrimaryKey
    @NonNull
    public String productId;

    public FavoriteEntity(@NonNull String productId) {
        this.productId = productId;
    }
}