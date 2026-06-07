package com.example.marketplace.utils;

import com.example.marketplace.model.Category;
import java.util.ArrayList;
import java.util.List;

public class CategoryHelper {

    // Trả về danh sách Object (Gồm Tên và Icon) dùng cho màn hình Home
    public static List<Category> getCategories() {
        List<Category> list = new ArrayList<>();
        list.add(new Category("Sách / Giáo trình", android.R.drawable.ic_menu_agenda));
        list.add(new Category("Điện tử / Công nghệ", android.R.drawable.ic_menu_slideshow));
        list.add(new Category("Đồ gia dụng", android.R.drawable.ic_menu_gallery));
        list.add(new Category("Đồ dùng cá nhân", android.R.drawable.ic_menu_camera));
        list.add(new Category("Khác", android.R.drawable.ic_menu_preferences));
        return list;
    }

    // Trả về danh sách Tên (String) dùng cho Dropdown lúc Đăng bài và Chips lúc Tìm kiếm
    public static String[] getCategoryNames() {
        List<Category> categories = getCategories();
        String[] names = new String[categories.size()];
        for (int i = 0; i < categories.size(); i++) {
            names[i] = categories.get(i).getName();
        }
        return names;
    }
}