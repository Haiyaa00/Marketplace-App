package com.example.marketplace.utils;

public class DateUtils {

    /**
     * Tính thời gian tương đối từ lúc đăng bài đến hiện tại
     */
    public static String getRelativeTimeSpan(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;
        if (diff < 0) diff = 0; // Tránh lỗi thời gian tương lai do lệch múi giờ

        long minutes = diff / (60 * 1000);
        long hours = diff / (60 * 60 * 1000);
        long days = diff / (24 * 60 * 60 * 1000);

        if (minutes < 1) {
            return "Vừa xong";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else {
            return days + " ngày trước";
        }
    }
}