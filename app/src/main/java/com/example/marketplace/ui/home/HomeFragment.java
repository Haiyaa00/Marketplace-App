package com.example.marketplace.ui.home;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.marketplace.R;
import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.repository.ProductRepository;
import com.example.marketplace.databinding.FragmentHomeBinding;
import com.example.marketplace.ui.detail.ProductDetailActivity;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;

    // Biến dùng để quản lý luồng dữ liệu, giúp chuyển đổi mượt mà giữa "Tất cả" và "Tìm kiếm"
    private LiveData<List<ProductEntity>> currentProductsLiveData;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Load banner image
        Glide.with(this)
                .load("https://img.freepik.com/premium-vector/online-marketplace-concept-people-buying-selling-online-store-flat-illustration_18660-3183.jpg")
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(binding.imgBanner);

        setupRecyclerViews();
        setupSwipeRefresh();
        setupSearchBar(); // Khởi tạo thanh tìm kiếm

        // Load toàn bộ dữ liệu lần đầu
        observeData(viewModel.getLocalProducts());

        // Gọi load data từ mạng về (Chạy ngầm)
        triggerRefresh();
    }

    private void setupRecyclerViews() {
        // 1. SETUP DANH MỤC
        List<com.example.marketplace.model.Category> mockCategories = new ArrayList<>();
        mockCategories.add(new com.example.marketplace.model.Category("Sách", android.R.drawable.ic_menu_agenda));
        mockCategories.add(new com.example.marketplace.model.Category("Điện tử", android.R.drawable.ic_menu_slideshow));
        mockCategories.add(new com.example.marketplace.model.Category("Đồ gia dụng", android.R.drawable.ic_menu_gallery));
        mockCategories.add(new com.example.marketplace.model.Category("Khác", android.R.drawable.ic_menu_preferences));

        binding.edtSearch.setFocusable(false); // Không cho bàn phím bật lên ở màn Home
        binding.edtSearch.setClickable(true);
        binding.edtSearch.setOnClickListener(v -> {
            androidx.navigation.Navigation.findNavController(requireView()).navigate(R.id.searchFragment);
        });

        // 2. SETUP SẢN PHẨM
        productAdapter = new ProductAdapter(false);
        binding.rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvProducts.setAdapter(productAdapter);

        productAdapter.setOnProductClickListener(product -> {
            // Tăng view
            new ProductRepository(requireActivity().getApplication()).incrementViewCount(product.id);
            // Chuyển sang chi tiết
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        });
    }

    // ================= TÍNH NĂNG TÌM KIẾM (SEARCH) OFFLINE REALTIME =================
    private void setupSearchBar() {
        binding.edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    // Nếu ô tìm kiếm trống -> Hiện tất cả sản phẩm
                    observeData(viewModel.getLocalProducts());
                } else {
                    // Nếu có chữ -> Chạy lệnh SQL LIKE qua ViewModel
                    observeData(viewModel.searchProducts(query));
                }
            }
        });
    }

    // MA THUẬT KIẾN TRÚC: Quản lý Observer thông minh chống rò rỉ RAM
    private void observeData(LiveData<List<ProductEntity>> newLiveData) {
        // Gỡ bỏ người lắng nghe cũ (để tránh 2 luồng dữ liệu đè nhau gây lag)
        if (currentProductsLiveData != null) {
            currentProductsLiveData.removeObservers(getViewLifecycleOwner());
        }

        // Gắn luồng dữ liệu mới vào
        currentProductsLiveData = newLiveData;
        currentProductsLiveData.observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                productAdapter.submitList(products);
            }
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright, android.R.color.holo_blue_dark);
        binding.swipeRefreshLayout.setOnRefreshListener(this::triggerRefresh);
    }

    private void triggerRefresh() {
        viewModel.refreshProducts().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    binding.swipeRefreshLayout.setRefreshing(true);
                    break;
                case SUCCESS:
                    binding.swipeRefreshLayout.setRefreshing(false);
                    // Không cần làm gì thêm vì Room DB tự động push data mới lên UI qua observeData()
                    break;
                case ERROR:
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu: " + resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}