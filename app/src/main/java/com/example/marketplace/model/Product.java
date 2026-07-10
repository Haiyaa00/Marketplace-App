package com.example.marketplace.model;

import java.util.List;

public class Product {
    private String id;
    private String sellerId;
    private String title;
    private double price;
    private String description;
    private String category;
    private String imageUrl; // Ảnh đại diện
    private List<String> imageUrls; // Danh sách tất cả ảnh
    private long timestamp;
    private String contactPhone;
    private String address;
    private int viewCount;

    // BẮT BUỘC: Constructor rỗng cho Firebase Firestore
    public Product() {
    }

    public Product(String id, String sellerId, String title, double price,
                   String description, String category, String imageUrl, List<String> imageUrls, long timestamp,
                   String contactPhone, String Address, int viewCount) {

        this.id = id;
        this.sellerId = sellerId;
        this.title = title;
        this.price = price;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.imageUrls = imageUrls;
        this.timestamp = timestamp;
        this.contactPhone = contactPhone;
        this.address = address;
        this.viewCount = viewCount;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getContactPhone() { return contactPhone; }
    public void setContactPhone(String contactPhone) { this.contactPhone = contactPhone; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

}
