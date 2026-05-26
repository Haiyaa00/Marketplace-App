package com.example.marketplace.ui.home;


import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.example.marketplace.databinding.FragmentHomeBinding;

public class HomeFragment extends Fragment {

    // Tránh Memory Leak khi dùng ViewBinding trong Fragment
    private FragmentHomeBinding binding;
    private HomeViewModel viewModel;

    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

        // Load banner image
        Glide.with(this)
             .load("https://img.freepik.com/premium-vector/online-marketplace-concept-people-buying-selling-online-store-flat-illustration_18660-3183.jpg")
             .placeholder(android.R.drawable.ic_menu_gallery)
             .into(binding.imgBanner);

        setupRecyclerViews();
        setupSwipeRefresh();
        observeData();

        // Gọi load data lần đầu tiên khi vừa vào màn hình
        triggerRefresh();
    }

    private void setupRecyclerViews() {
        java.util.List<com.example.marketplace.model.Category> mockCategories = new java.util.ArrayList<>();
        mockCategories.add(new com.example.marketplace.model.Category("Giáo trình", android.R.drawable.ic_menu_agenda));
        mockCategories.add(new com.example.marketplace.model.Category("Phòng trọ", android.R.drawable.ic_menu_compass));
        mockCategories.add(new com.example.marketplace.model.Category("Điện tử", android.R.drawable.ic_menu_slideshow));
        mockCategories.add(new com.example.marketplace.model.Category("Đồ gia dụng", android.R.drawable.ic_menu_gallery));
        mockCategories.add(new com.example.marketplace.model.Category("Khác", android.R.drawable.ic_menu_preferences));

        // Thiết lập Adapter cho Danh mục
        productAdapter = new ProductAdapter(false);
        binding.rvProducts.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2));
        binding.rvProducts.setAdapter(productAdapter);
        categoryAdapter = new CategoryAdapter(mockCategories);
        binding.rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvCategories.setAdapter(categoryAdapter); // Gán Adapter thành công!

        // Khởi tạo và gán Adapter cho Products RecyclerView
        productAdapter.setOnProductClickListener(product -> {
            // 1. GỌI HÀM TĂNG VIEW LÊN FIRESTORE
            new com.example.marketplace.data.repository.ProductRepository(requireActivity().getApplication())
                    .incrementViewCount(product.id);

            // 2. CHUYỂN SANG MÀN DETAIL
            android.content.Intent intent = new android.content.Intent(requireContext(), com.example.marketplace.ui.detail.ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        });
    }

    private void setupSwipeRefresh() {
        // Cài đặt màu vòng xoay cho trùng tông Ocean Blue
        binding.swipeRefreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_blue_dark
        );

        // Lắng nghe sự kiện người dùng vuốt từ trên xuống
        binding.swipeRefreshLayout.setOnRefreshListener(this::triggerRefresh);
    }

    private void triggerRefresh() {
        // Gọi hàm làm mới từ Firebase
        viewModel.refreshProducts().observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case LOADING:
                    // swipeRefreshLayout tự quay, hoặc có thể bật Shimmer ở đây
                    binding.swipeRefreshLayout.setRefreshing(true);
                    break;

                case SUCCESS:
                    binding.swipeRefreshLayout.setRefreshing(false);
                    // Dữ liệu mới đã được repository lưu đè xuống Room,
                    // observeData() sẽ tự động nhận được data mới!
                    break;

                case ERROR:
                    binding.swipeRefreshLayout.setRefreshing(false);
                    Toast.makeText(requireContext(), "Lỗi tải dữ liệu: " + resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void observeData() {
        viewModel.getLocalProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null && !products.isEmpty()) {
                productAdapter.submitList(products); // MỞ COMMENT DÒNG NÀY
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null; // CỰC KỲ QUAN TRỌNG: Ngăn chặn Memory Leak (Tràn bộ nhớ)
    }
}
