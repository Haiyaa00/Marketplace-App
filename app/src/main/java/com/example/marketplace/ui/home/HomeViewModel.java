package com.example.marketplace.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.remote.FirebaseManager;
import com.example.marketplace.data.repository.ProductRepository;
import com.example.marketplace.utils.Resource;

import java.util.List;

public class HomeViewModel extends AndroidViewModel {

    private final ProductRepository repository;

    // LiveData chứa danh sách sản phẩm lấy từ Room (Local)
    private final LiveData<List<ProductEntity>> localProducts;
    private androidx.lifecycle.MutableLiveData<List<String>> bannerUrls;

    public LiveData<List<String>> getBannerUrls() {
        androidx.lifecycle.MutableLiveData<List<String>> liveData = new androidx.lifecycle.MutableLiveData<>();

        FirebaseManager.getInstance().getDb().collection("Banners").get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<String> urls = new java.util.ArrayList<>();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String url = doc.getString("imageUrl");
                        if (url != null) {
                            urls.add(url);
                        }
                    }
                    liveData.setValue(urls);
                })
                .addOnFailureListener(e -> {
                    // Nếu lỗi mạng, trả về mảng trống để tránh crash
                    liveData.setValue(new java.util.ArrayList<>());
                });

        return liveData;
    }


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

    public LiveData<List<ProductEntity>> searchProducts(String query) {
        return repository.searchProductsLocally(query);
    }

    public LiveData<List<ProductEntity>> getProductsPaginated(int page, int pageSize) {
        return repository.getProductsPaginated(page, pageSize);
    }
}
