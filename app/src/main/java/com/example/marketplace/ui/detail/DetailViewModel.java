package com.example.marketplace.ui.detail;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.remote.FirebaseManager;
import com.example.marketplace.data.repository.ProductRepository;
import com.example.marketplace.model.User;
import com.google.firebase.auth.FirebaseAuth;

public class DetailViewModel extends AndroidViewModel {

    private final ProductRepository productRepository;
    private final FirebaseManager firebaseManager;

    public DetailViewModel(@NonNull Application application) {
        super(application);
        productRepository = new ProductRepository(application);
        firebaseManager = FirebaseManager.getInstance();
    }

    public LiveData<ProductEntity> getProductDetails(String productId) {
        return productRepository.getProductByIdLocally(productId);
    }

    // Lấy thông tin người bán từ Firestore theo UID
    public LiveData<User> getSellerInfo(String sellerId) {
        MutableLiveData<User> sellerLiveData = new MutableLiveData<>();
        firebaseManager.getUserInfo(sellerId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                sellerLiveData.setValue(doc.toObject(User.class));
            }
        });
        return sellerLiveData;
    }

    public LiveData<Boolean> isFavorite(String productId) {
        // Lấy ID của người dùng đang đăng nhập hiện tại
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            return productRepository.isFavorite(userId, productId);
        }
        return null;
    }

    public void toggleFavorite(String productId, boolean isCurrentlyFavorite) {
        productRepository.toggleFavorite(productId, isCurrentlyFavorite);
    }
}