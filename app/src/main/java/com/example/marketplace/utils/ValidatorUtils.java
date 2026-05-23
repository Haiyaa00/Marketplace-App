package com.example.marketplace.utils;

import android.util.Patterns;

public class ValidatorUtils {

    // Thay đổi hằng số tên miền của trường
    private static final String HAU_EMAIL_SUFFIX = "@kientruchanoi.edu.vn";

    public static boolean isValidEduEmail(String email) {
        if (email == null || email.isEmpty()) {
            return false;
        }
        // Kiểm tra xem có đúng định dạng email và kết thúc bằng đuôi của trường không
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
                && email.toLowerCase().endsWith(HAU_EMAIL_SUFFIX);
    }
}