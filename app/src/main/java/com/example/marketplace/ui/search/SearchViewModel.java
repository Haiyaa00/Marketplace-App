package com.example.marketplace.ui.search;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.repository.ProductRepository;
import java.util.List;

public class SearchViewModel extends AndroidViewModel {
    private final ProductRepository repository;

    public SearchViewModel(@NonNull Application application) {
        super(application);
        repository = new ProductRepository(application);
    }

    // Truyền Từ khóa và Danh mục. Nếu không có gì, truyền chuỗi rỗng "".
    public LiveData<List<ProductEntity>> searchAndFilter(String query, String category, int page, int pageSize) {
        return repository.searchAndFilterProductsPaginated(query, category, page, pageSize);
    }
}