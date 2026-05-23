package com.example.marketplace.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ImageUtils {

    /**
     * Nén ảnh được chọn từ Uri về chất lượng 80% dạng JPEG và lưu vào file tạm thời (Cache)
     */
    public static Uri compressImage(Context context, Uri imageUri) {
        try {
            // 1. Đọc Uri thành Bitmap
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) return imageUri;

            // 2. Tạo một file tạm thời trong thư mục Cache của ứng dụng
            File tempFile = new File(context.getCacheDir(), "temp_avatar_compressed.jpg");
            FileOutputStream fos = new FileOutputStream(tempFile);

            // 3. Thực hiện nén chất lượng ảnh về 80% (vừa sắc nét vừa cực nhẹ, khoảng dưới 200KB)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            // Trả về Uri của file đã nén
            return Uri.fromFile(tempFile);

        } catch (Exception e) {
            e.printStackTrace();
            return imageUri; // Nếu nén lỗi thì trả về Uri gốc làm dự phòng
        }
    }
}