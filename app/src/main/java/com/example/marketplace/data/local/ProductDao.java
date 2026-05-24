package com.example.marketplace.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProducts(List<ProductEntity> products);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertProduct(ProductEntity product);

    // Lấy danh sách sản phẩm, sắp xếp mới nhất lên đầu
    @Query("SELECT * FROM products ORDER BY timestamp DESC")
    LiveData<List<ProductEntity>> getAllProducts();

    // Query LIKE tìm kiếm title offline
    @Query("SELECT * FROM products WHERE title LIKE '%' || :searchQuery || '%' ORDER BY timestamp DESC")
    LiveData<List<ProductEntity>> searchProducts(String searchQuery);

    // Lấy danh sách bài đăng của chính user
    @Query("SELECT * FROM products WHERE sellerId = :userId ORDER BY timestamp DESC")
    LiveData<List<ProductEntity>> getMyProducts(String userId);

    @Query("DELETE FROM products")
    void clearAllProducts();

    @Query("DELETE FROM products WHERE id = :productId")
    void deleteProductById(String productId);

    @Query("SELECT * FROM products WHERE id = :productId LIMIT 1")
    LiveData<ProductEntity> getProductById(String productId);
}
