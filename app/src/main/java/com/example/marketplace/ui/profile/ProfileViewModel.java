package com.example.marketplace.ui.profile;

import android.app.Application;
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
import com.example.marketplace.utils.Resource;

import java.util.List;

public class ProfileViewModel extends AndroidViewModel {

    private final AuthRepository authRepository;
    private final ProductRepository productRepository;
    private final LiveData<UserEntity> currentUser;

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        authRepository = new AuthRepository(application);
        productRepository = new ProductRepository(application);
        currentUser = authRepository.getCachedUser();
    }

    public LiveData<UserEntity> getCurrentUser() {
        return currentUser;
    }

    public LiveData<List<ProductEntity>> getMyProducts(String userId) {
        return productRepository.getMyProducts(userId);
    }

    public void logout() {
        authRepository.logout();
    }

    public LiveData<Resource<Void>> deleteProduct(String productId) {
        return productRepository.deleteProduct(productId);
    }

    public LiveData<Resource<Void>> updateProfile(UserEntity currentUser, String newName, String newPhone) {
        return authRepository.updateProfile(currentUser, newName, newPhone);
    }

    // ================= CHỨC NĂNG TẢI ẢNH ĐẠI DIỆN LÊN CLOUD =================

    // 1. Đẩy ảnh lên Cloudinary và trả về LiveData chứa URL ảnh
    public LiveData<Resource<String>> uploadAvatarToCloud(Uri imageUri) {
        MutableLiveData<Resource<String>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        CloudinaryManager.getInstance().uploadImage(imageUri).addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                result.setValue(Resource.success(task.getResult()));
            } else {
                result.setValue(Resource.error(task.getException() != null ?
                        task.getException().getMessage() : "Lỗi upload ảnh lên Cloudinary", null));
            }
        });
        return result;
    }

    // 2. Ghi đè URL ảnh vừa lấy được vào CSDL
    public LiveData<Resource<Void>> updateAvatarUrl(UserEntity currentUser, String newAvatarUrl) {
        return authRepository.updateAvatar(currentUser, newAvatarUrl);
    }
}