package com.example.marketplace.data.remote;

import android.net.Uri;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.Map;

public class CloudinaryManager {

    private static volatile CloudinaryManager INSTANCE;

    // Thay bằng tên Preset bạn đã tạo ở Bước 1
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
     * Upload ảnh lên Cloudinary và trả về Task chứa URL an toàn (https)
     */
    public Task<String> uploadImage(Uri imageUri) {
        TaskCompletionSource<String> tcs = new TaskCompletionSource<>();

        MediaManager.get().upload(imageUri)
                .unsigned(UPLOAD_PRESET)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        // Bắt đầu upload
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        // Đang upload (Có thể bỏ qua)
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Upload thành công, lấy secure_url (link ảnh https)
                        String imageUrl = (String) resultData.get("secure_url");
                        tcs.setResult(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        // Upload thất bại
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