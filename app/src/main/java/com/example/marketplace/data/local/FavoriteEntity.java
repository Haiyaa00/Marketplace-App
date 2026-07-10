package com.example.marketplace.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;

// Bổ sung userId và dùng Khóa chính kép (Composite Key)
@Entity(tableName = "favorites", primaryKeys = {"userId", "productId"})
public class FavoriteEntity {

    @NonNull
    public String userId;

    @NonNull
    public String productId;

    public FavoriteEntity(@NonNull String userId, @NonNull String productId) {
        this.userId = userId;
        this.productId = productId;
    }
}