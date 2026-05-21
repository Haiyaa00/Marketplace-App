package com.example.marketplace.model;

public class User {
    private String uid;
    private String name;
    private String email;
    private String phone;
    private String avatarUrl;

    // BẮT BUỘC: Constructor rỗng cho Firebase Firestore
    public User() {
    }

    public User(String uid, String name, String email, String phone, String avatarUrl) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
    }

    // Getters and Setters
    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
}
