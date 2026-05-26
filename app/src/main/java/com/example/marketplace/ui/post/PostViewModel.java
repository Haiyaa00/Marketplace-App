package com.example.marketplace.ui.post;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.local.UserEntity;
import com.example.marketplace.data.remote.CloudinaryManager;
import com.example.marketplace.data.repository.AuthRepository;
import com.example.marketplace.data.repository.ProductRepository;
import com.example.marketplace.model.Product;
import com.example.marketplace.utils.ImageUtils;
import com.example.marketplace.utils.Resource;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.List;

public class PostViewModel extends AndroidViewModel {

    private final ProductRepository productRepository;
    private final AuthRepository authRepository;

    public PostViewModel(@NonNull Application application) {
        super(application);
        productRepository = new ProductRepository(application);
        authRepository = new AuthRepository(application);
    }

    // 1. Lấy thông tin user hiện tại (Để lấy UID làm sellerId)
    public LiveData<UserEntity> getCurrentUser() {
        return authRepository.getCachedUser();
    }

    // 2. Upload ảnh sản phẩm lên Cloudinary
    public LiveData<Resource<List<String>>> uploadMultipleImagesToCloud(Context context, List<Uri> imageUris) {
        MutableLiveData<Resource<List<String>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        List<com.google.android.gms.tasks.Task<String>> uploadTasks = new java.util.ArrayList<>();

        for (Uri uri : imageUris) {
            Uri compressedUri = ImageUtils.compressImage(context, uri);
            uploadTasks.add(CloudinaryManager.getInstance().uploadImage(context, compressedUri));
        }

        com.google.android.gms.tasks.Tasks.whenAllSuccess(uploadTasks).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                List<String> uploadedUrls = new java.util.ArrayList<>();
                for (Object obj : task.getResult()) {
                    uploadedUrls.add((String) obj);
                }
                result.setValue(Resource.success(uploadedUrls));
            } else {
                result.setValue(Resource.error("Lỗi tải ảnh: " + task.getException().getMessage(), null));
            }
        });

        return result;
    }

    // 3. Đăng sản phẩm mới lên hệ thống
    public LiveData<Resource<Void>> createProduct(Product product) {
        return productRepository.createProduct(product);
    }

    public LiveData<List<ProductEntity>> getMyProducts(String userId) {
        return productRepository.getMyProducts(userId);
    }

    public LiveData<Resource<Void>> deleteProduct(String productId) {
        return productRepository.deleteProduct(productId);
    }
}