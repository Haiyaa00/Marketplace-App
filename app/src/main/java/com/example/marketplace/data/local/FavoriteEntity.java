package com.example.marketplace.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "favorites",
        foreignKeys = @ForeignKey(
                entity = ProductEntity.class,
                parentColumns = "id",
                childColumns = "productId",
                onDelete = ForeignKey.CASCADE
        )
)
public class FavoriteEntity {
    @PrimaryKey
    @NonNull
    public String productId;

    public FavoriteEntity(@NonNull String productId) {
        this.productId = productId;
    }
}
