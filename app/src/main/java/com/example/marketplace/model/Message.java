package com.example.marketplace.model;

public class Message {
    private String senderId;
    private String text;
    private long timestamp;
    private String imageUrl;

    public Message() {} // Dành cho Firebase

    public Message(String senderId, String text, String imageUrl, long timestamp) {
        this.senderId = senderId;
        this.text = text;
        this.timestamp = timestamp;
        this.imageUrl = imageUrl;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}