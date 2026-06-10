package com.example.marketplace.utils;

import android.util.Patterns;

public class ValidatorUtils {
    public static final String HAU_EMAIL_SUFFIX = "@kientruchanoi.edu.vn";

    public static boolean isValidEduEmail(String input) {
        if (input == null || input.isEmpty()) {
            return false;
        }
        // Nếu người dùng nhập đầy đủ email
        if (input.contains("@")) {
            return Patterns.EMAIL_ADDRESS.matcher(input).matches()
                    && input.toLowerCase().endsWith(HAU_EMAIL_SUFFIX);
        }

        // Nếu chỉ nhập mã sinh viên / tên đăng nhập (không chứa dấu cách, @, ...)
        // Chấp nhận chữ cái, số, dấu chấm, gạch dưới, gạch ngang
        return input.matches("^[a-zA-Z0-9._-]+$");
    }

    /**
     * Chuyển đổi mã sinh viên hoặc email chưa đầy đủ thành email của trường.
     */
    public static String formatToEduEmail(String input) {
        if (input == null || input.isEmpty()) return "";
        String lowercaseInput = input.trim().toLowerCase();
        if (lowercaseInput.endsWith(HAU_EMAIL_SUFFIX)) {
            return lowercaseInput;
        }
        return lowercaseInput + HAU_EMAIL_SUFFIX;
    }

    /**
     * Kiểm tra mật khẩu hợp lệ (tối thiểu 6 ký tự).
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Kiểm tra chuỗi không được để trống.
     */
    public static boolean isNotEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Kiểm tra số điện thoại hợp lệ (định dạng Việt Nam).
     */
    public static boolean isValidPhoneNumber(String phone) {
        if (phone == null || phone.isEmpty()) return false;
        return Patterns.PHONE.matcher(phone).matches() && phone.length() >= 10 && phone.length() <= 11;
    }

    /**
     * Kiểm tra xác nhận mật khẩu khớp với mật khẩu.
     */
    public static boolean doPasswordsMatch(String password, String confirmPassword) {
        return password != null && password.equals(confirmPassword);
    }
}