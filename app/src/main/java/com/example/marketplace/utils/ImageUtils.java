package com.example.marketplace.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.UUID;

public class ImageUtils {

    public static Uri compressImage(Context context, Uri imageUri) {
        try {
            InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            if (bitmap == null) return imageUri;

            // FIX LỖI TRÙNG ẢNH: Tạo tên file Random bằng UUID
            String uniqueFileName = "img_compressed_" + UUID.randomUUID().toString() + ".jpg";
            File tempFile = new File(context.getCacheDir(), uniqueFileName);

            FileOutputStream fos = new FileOutputStream(tempFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos);
            fos.flush();
            fos.close();

            return Uri.fromFile(tempFile);
        } catch (Exception e) {
            e.printStackTrace();
            return imageUri;
        }
    }
}