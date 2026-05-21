package com.example.marketplace.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "products")
public class ProductEntity {
    @PrimaryKey
    @NonNull
    public String id;

    public String sellerId;
    public String title;
    public double price;
    public String description;
    public String category;
    public String imageUrl;
    public long timestamp;

    public ProductEntity(@NonNull String id, String sellerId, String title, double price,
                         String description, String category, String imageUrl, long timestamp) {
        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.price = price;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
    }
}
