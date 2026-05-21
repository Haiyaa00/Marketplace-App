package com.example.marketplace.utils;

public class ValidatorUtils {
    //Kiểm tra định dạng mail sinh viên
    public static boolean isValidEduEmail(String email) {
        if(email == null || email.trim().isEmpty()){
            return false;
        }
        return email.trim().toLowerCase().endsWith("@edu.vn");
    }
}
