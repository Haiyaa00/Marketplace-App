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

    public static String getChatDateLabel(long timestamp) {
        java.util.Calendar now = java.util.Calendar.getInstance();
        java.util.Calendar msgTime = java.util.Calendar.getInstance();
        msgTime.setTimeInMillis(timestamp);

        // Nếu cùng Năm
        if (now.get(java.util.Calendar.YEAR) == msgTime.get(java.util.Calendar.YEAR)) {
            // Cùng ngày trong năm
            if (now.get(java.util.Calendar.DAY_OF_YEAR) == msgTime.get(java.util.Calendar.DAY_OF_YEAR)) {
                return "Hôm nay";
            }
            // Cách nhau 1 ngày
            else if (now.get(java.util.Calendar.DAY_OF_YEAR) - msgTime.get(java.util.Calendar.DAY_OF_YEAR) == 1) {
                return "Hôm qua";
            }
        }

        // Trả về ngày tháng chuẩn nếu đã cũ hơn
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
        return sdf.format(new java.util.Date(timestamp));
    }

    // Hàm kiểm tra 2 mốc thời gian có nằm trong cùng 1 ngày không
    public static boolean isSameDay(long ts1, long ts2) {
        java.util.Calendar cal1 = java.util.Calendar.getInstance();
        cal1.setTimeInMillis(ts1);
        java.util.Calendar cal2 = java.util.Calendar.getInstance();
        cal2.setTimeInMillis(ts2);

        return cal1.get(java.util.Calendar.YEAR) == cal2.get(java.util.Calendar.YEAR) &&
                cal1.get(java.util.Calendar.DAY_OF_YEAR) == cal2.get(java.util.Calendar.DAY_OF_YEAR);
    }
}