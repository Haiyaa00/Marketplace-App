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

    // Lệnh SQL thông minh: Lọc từ khóa (Tên/Mô tả) VÀ Lọc Danh mục (Nếu category rỗng thì lấy tất cả)
    @Query("SELECT * FROM products WHERE (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%') " +
            "AND (:category = '' OR category = :category) ORDER BY timestamp DESC")
    LiveData<List<ProductEntity>> searchAndFilterProducts(String searchQuery, String category);

    // 1. Lấy sản phẩm trang chủ phân trang (Sử dụng LIMIT và OFFSET) [1]
    @Query("SELECT * FROM products ORDER BY timestamp DESC LIMIT :pageSize OFFSET :pageOffset")
    LiveData<List<ProductEntity>> getProductsPaginated(int pageSize, int pageOffset);

    @Query("SELECT * FROM products WHERE (title LIKE '%' || :searchQuery || '%' OR description LIKE '%' || :searchQuery || '%') " +
            "AND (:category = '' OR category = :category) ORDER BY timestamp DESC LIMIT :pageSize OFFSET :pageOffset")
    LiveData<List<ProductEntity>> searchAndFilterProductsPaginated(String searchQuery, String category, int pageSize, int pageOffset);
}