package com.example.marketplace.data.local;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.util.List;

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
    public List<String> imageUrls;
    public long timestamp;
    public String contactPhone;
    public String address;
    public int viewCount;

    public ProductEntity(@NonNull String id, String sellerId, String title, double price,
                         String description, String category, List<String> imageUrls, long timestamp,
                         String contactPhone, String address, int viewCount) {
        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.price = price;
        this.description = description;
        this.category = category;
        this.imageUrls = imageUrls;
        this.timestamp = timestamp;
        this.contactPhone = contactPhone;
        this.address = address;
        this.viewCount = viewCount;
    }
}
