package com.example.marketplace.data.local;


import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    public String uid;

    public String name;
    public String email;
    public String phone;
    public String avatarUrl;

    public UserEntity(@NonNull String uid, String name, String email, String phone, String avatarUrl) {
        this.uid = uid;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.avatarUrl = avatarUrl;
    }
}
