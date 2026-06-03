package com.example.marketplace.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.marketplace.data.local.FavoriteDao;
import com.google.firebase.firestore.DocumentSnapshot;
import com.example.marketplace.data.local.AppDatabase;
import com.example.marketplace.data.local.ProductDao;
import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.remote.FirebaseManager;
import com.example.marketplace.model.Product;
import com.example.marketplace.utils.DataMapper;
import com.example.marketplace.utils.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProductRepository {

    private final FirebaseManager firebaseManager;
    private final ProductDao productDao;
    private final ExecutorService executor;
    private final FavoriteDao favoriteDao;

    public ProductRepository(Application application) {
        this.firebaseManager = FirebaseManager.getInstance();
        AppDatabase db = AppDatabase.getInstance(application);
        this.productDao = db.productDao();
        this.executor = Executors.newFixedThreadPool(2);
        this.favoriteDao = db.favoriteDao();
    }

    // ================== OFFLINE-FIRST READ ==================

    public LiveData<List<ProductEntity>> getAllProductsLocally() {
        return productDao.getAllProducts();
    }

    public LiveData<List<ProductEntity>> searchProductsLocally(String query) {
        return productDao.searchProducts(query);
    }

    public LiveData<List<ProductEntity>> getMyProducts(String userId) {
        return productDao.getMyProducts(userId);
    }

    public LiveData<ProductEntity> getProductByIdLocally(String productId) {
        return productDao.getProductById(productId);
    }

    public void incrementViewCount(String productId) {
        // Tăng view trên Firestore (Bảo mật: Atomic operation)
        firebaseManager.getDb().collection("Products").document(productId)
                .update("viewCount", com.google.firebase.firestore.FieldValue.increment(1));
    }
    // ========================================================

    // ================== REMOTE FETCH & SYNC ==================

    public LiveData<Resource<Void>> refreshProducts() {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseManager.getAllProducts().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Product> fetchedProducts = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Product product = doc.toObject(Product.class);
                    if (product != null) {
                        // Giải pháp chống crash nếu id bị null từ Firestore
                        if (product.getId() == null || product.getId().isEmpty()) {
                            product.setId(doc.getId());
                        }
                        fetchedProducts.add(product);
                    }
                }

                // Đồng bộ xuống local sqlite
                executor.execute(() -> {
                    productDao.insertProducts(DataMapper.mapToProductEntityList(fetchedProducts));
                    result.postValue(Resource.success(null));
                });
            } else {
                result.setValue(Resource.error(task.getException() != null ?
                        task.getException().getMessage() : "Failed to fetch products", null));
            }
        });

        return result;
    }

    // ================== CREATE PRODUCT ==================

    public LiveData<Resource<Void>> createProduct(Product product) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseManager.saveProduct(product).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                executor.execute(() -> {
                    productDao.insertProduct(DataMapper.mapToProductEntity(product));
                    result.postValue(Resource.success(null));
                });
            } else {
                result.setValue(Resource.error(task.getException() != null ?
                        task.getException().getMessage() : "Failed to save product", null));
            }
        });
        return result;
    }

    public LiveData<Resource<Void>> deleteProduct(String productId) {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        // 1. Xóa trên Firestore
        firebaseManager.deleteProduct(productId).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // 2. Xóa trên SQLite cục bộ
                executor.execute(() -> {
                    productDao.deleteProductById(productId);
                    result.postValue(Resource.success(null));
                });
            } else {
                result.setValue(Resource.error(task.getException() != null ?
                        task.getException().getMessage() : "Lỗi khi xóa bài đăng!", null));
            }
        });
        return result;
    }

    public LiveData<List<ProductEntity>> getFavoriteProducts() {
        return favoriteDao.getFavoriteProducts();
    }

    public LiveData<Boolean> isFavorite(String productId) {
        return favoriteDao.isFavorite(productId);
    }

    public void toggleFavorite(String productId, boolean isCurrentlyFavorite) {
        executor.execute(() -> {
            if (isCurrentlyFavorite) {
                favoriteDao.removeFavorite(productId);
            } else {
                favoriteDao.insertFavorite(new com.example.marketplace.data.local.FavoriteEntity(productId));
            }
        });
    }
}