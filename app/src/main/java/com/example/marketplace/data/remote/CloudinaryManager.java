package com.example.marketplace.data.remote;

import android.content.Context;
import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryManager {

    private static volatile CloudinaryManager INSTANCE;
    private static boolean isInitialized = false; // Cờ kiểm soát khởi tạo

    // 1. ĐIỀN THÔNG TIN CLOUDINARY CỦA BẠN VÀO ĐÂY (MỘT NƠI DUY NHẤT)
    private static final String CLOUD_NAME = "da1p50owx";       // Thay bằng Cloud Name trên web của bạn (vd: dpxxxxxxx)
    private static final String UPLOAD_PRESET = "student_presets"; // Tên preset bạn đã đặt trên web

    private CloudinaryManager() {}

    public static CloudinaryManager getInstance() {
        if (INSTANCE == null) {
            synchronized (CloudinaryManager.class) {
                if (INSTANCE == null) {
                    INSTANCE = new CloudinaryManager();
                }
            }
        }
        return INSTANCE;
    }

    // Tự động kiểm tra và khởi tạo Cloudinary không phụ thuộc MyApplication
    private void initCloudinary(Context context) {
        if (!isInitialized) {
            try {
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", CLOUD_NAME);
                MediaManager.init(context.getApplicationContext(), config);
                isInitialized = true;
            } catch (Exception e) {
                isInitialized = true; // Bỏ qua nếu hệ thống đã được init trước đó
            }
        }
    }

    /**
     * Upload ảnh lên Cloudinary (Yêu cầu truyền Context để tự động init)
     */
    public Task<String> uploadImage(Context context, Uri imageUri) {
        initCloudinary(context); // Khởi tạo tự động bằng Context

        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        MediaManager.get().upload(imageUri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        tcs.setResult(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        tcs.setException(new Exception(error.getDescription()));
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        tcs.setException(new Exception("Upload rescheduled: " + error.getDescription()));
                    }
                })
                .dispatch();

        return tcs.getTask();
    }
}