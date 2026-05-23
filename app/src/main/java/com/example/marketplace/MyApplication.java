package com.example.marketplace;

import android.app.Application;
import com.cloudinary.android.MediaManager;
import java.util.HashMap;
import java.util.Map;

public class MyApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        // Khởi tạo Cloudinary
        Map<String, Object> config = new HashMap<>();
        config.put("cloud_name", "da1p50owx");
        // Không cần api_key và api_secret vì chúng ta dùng Unsigned Upload

        MediaManager.init(this, config);
    }
}
