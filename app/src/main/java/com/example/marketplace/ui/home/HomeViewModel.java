package com.example.marketplace.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.repository.ProductRepository;
import com.example.marketplace.utils.Resource;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final ProductRepository repository;

    // LiveData chứa danh sách sản phẩm lấy từ Room (Local)
    private final LiveData<List<ProductEntity>> localProducts;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(application);
        localProducts = repository.getAllProductsLocally();
    }

    // UI (Fragment) sẽ observe hàm này để lấy data hiển thị ngay lập tức
    public LiveData<List<ProductEntity>> getLocalProducts() {
        return localProducts;
    }

    // Hàm gọi Firebase kéo data mới đè xuống Room (dùng cho SwipeRefresh)
    public LiveData<Resource<Void>> refreshProducts() {
        return repository.refreshProducts();
    }
}
