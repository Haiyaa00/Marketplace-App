package com.example.marketplace.model;

public class Category {
    private String name;
    private int iconResId; // Lưu id icon hệ thống

    public Category(String name, int iconResId) {
        this.name = name;
        this.iconResId = iconResId;
    }

    public String getName() { return name; }
    public int getIconResId() { return iconResId; }
}