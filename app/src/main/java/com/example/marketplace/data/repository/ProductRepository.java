package com.example.marketplace.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

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

    public ProductRepository(Application application) {
        this.firebaseManager = FirebaseManager.getInstance();
        AppDatabase db = AppDatabase.getInstance(application);
        this.productDao = db.productDao();
        this.executor = Executors.newFixedThreadPool(2); // Cấp 2 luồng để xử lý DB nhanh hơn
    }

    // ================== OFFLINE-FIRST READ ==================

    // View (UI) sẽ luôn gọi hàm này để lấy LiveData. Room sẽ tự notify khi có data thay đổi.
    public LiveData<List<ProductEntity>> getAllProductsLocally() {
        return productDao.getAllProducts();
    }

    public LiveData<List<ProductEntity>> searchProductsLocally(String query) {
        // Logic search offline realtime với LIKE
        return productDao.searchProducts(query);
    }

    // ================== REMOTE FETCH & SYNC ==================

    // Hàm kéo dữ liệu mới nhất từ Firestore về và lưu đè xuống Room (Pull to Refresh)
    public LiveData<Resource<Void>> refreshProducts() {
        MutableLiveData<Resource<Void>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));

        firebaseManager.getAllProducts().addOnCompleteListener(task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                List<Product> fetchedProducts = new ArrayList<>();
                for (DocumentSnapshot doc : task.getResult().getDocuments()) {
                    Product product = doc.toObject(Product.class);
                    if (product != null) {
                        fetchedProducts.add(product);
                    }
                }

                // Đồng bộ xuống Local (Xóa dữ liệu cũ nếu muốn đảm bảo đồng bộ hoàn toàn, hoặc cứ để REPLACE đè lên)
                executor.execute(() -> {
                    // productDao.clearAllProducts(); // Mở comment dòng này nếu muốn xóa sạch cache cũ mỗi lần refresh
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

    // Tạo sản phẩm mới (Đẩy lên Firebase -> Xong thì push xuống Local Room ngay lập tức)
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
}
