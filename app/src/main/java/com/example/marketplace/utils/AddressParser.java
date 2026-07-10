package com.example.marketplace.utils;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AddressParser {

    // Map chứa K-V: Tên Quận -> Danh sách Phường
    private static Map<String, List<String>> districtData = new HashMap<>();

    public static void loadHanoiData(Context context) {
        if (!districtData.isEmpty()) return; // Nếu đã load rồi thì thôi

        try {
            // Đọc file hanoi_address.json từ thư mục assets
            InputStream is = context.getAssets().open("hanoi_address.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();

            String jsonString = new String(buffer, StandardCharsets.UTF_8);
            JSONArray jsonArray = new JSONArray(jsonString);

            // Phân tích JSON đưa vào Map
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String districtName = obj.getString("district");

                JSONArray wardsArray = obj.getJSONArray("wards");
                List<String> wardsList = new ArrayList<>();
                for (int j = 0; j < wardsArray.length(); j++) {
                    wardsList.add(wardsArray.getString(j));
                }

                districtData.put(districtName, wardsList);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static List<String> getDistricts() {
        return new ArrayList<>(districtData.keySet());
    }

    public static List<String> getWards(String districtName) {
        return districtData.containsKey(districtName) ? districtData.get(districtName) : new ArrayList<>();
    }
}