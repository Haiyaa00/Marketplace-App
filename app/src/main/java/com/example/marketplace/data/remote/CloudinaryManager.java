package com.example.marketplace.data.remote;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.HashMap;
import java.util.Map;

/**
 * CloudinaryManager - Senior Developer Version
 * Quản lý việc tải ảnh lên Cloudinary với cơ chế tự động khởi tạo và Singleton an toàn.
 */
public class CloudinaryManager {

    private static final String TAG = "CloudinaryManager";
    private static volatile CloudinaryManager INSTANCE;
    private static boolean isInitialized = false;

    // Cấu hình Cloudinary
    private static final String CLOUD_NAME = "da1p50owx";
    private static final String UPLOAD_PRESET = "student_presets";

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

    /**
     * Khởi tạo Cloudinary một cách an toàn.
     */
    private void initCloudinary(Context context) {
        if (!isInitialized) {
            try {
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", CLOUD_NAME);
                // Sử dụng Application Context để tránh rò rỉ bộ nhớ
                MediaManager.init(context.getApplicationContext(), config);
                Log.d(TAG, "Cloudinary initialized successfully");
                isInitialized = true;
            } catch (IllegalStateException e) {
                // MediaManager ném lỗi này nếu đã được khởi tạo trước đó
                Log.w(TAG, "Cloudinary was already initialized: " + e.getMessage());
                isInitialized = true; 
            } catch (Exception e) {
                Log.e(TAG, "Critical error during Cloudinary initialization: " + e.getMessage());
            }
        }
    }

    /**
     * Upload ảnh lên Cloudinary và trả về một Task chứa URL.
     */
    public Task<String> uploadImage(Context context, Uri imageUri) {
        initCloudinary(context);

        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        if (imageUri == null) {
            tcs.setException(new IllegalArgumentException("Image URI cannot be null"));
            return tcs.getTask();
        }

        MediaManager.get().upload(imageUri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Logic theo dõi tiến độ có thể thêm ở đây
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String imageUrl = (String) resultData.get("secure_url");
                        Log.i(TAG, "Upload success: " + imageUrl);
                        tcs.setResult(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        String desc = error != null ? error.getDescription() : "Unknown error";
                        Log.e(TAG, "Upload error: " + desc);
                        tcs.setException(new Exception(desc));
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.w(TAG, "Upload rescheduled: " + requestId);
                    }
                })
                .dispatch();

        return tcs.getTask();
    }
}
