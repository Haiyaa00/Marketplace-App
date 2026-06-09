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
    private View rootView;
    private boolean isInitialized = false;

    // Biến dùng để quản lý luồng dữ liệu, giúp chuyển đổi mượt mà giữa "Tất cả" và "Tìm kiếm"
    private LiveData<List<ProductEntity>> currentProductsLiveData;

    private android.os.Handler sliderHandler = new android.os.Handler();
    private List<String> bannerList;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        //CACHE VIEW: Nếu View chưa tồn tại thì mới tạo
        if (rootView == null) {
            binding = FragmentHomeBinding.inflate(inflater, container, false);
            rootView = binding.getRoot();
        }
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        //NGĂN CHẶN LOAD LẠI DATA: Chỉ chạy setup 1 lần duy nhất!
        if (!isInitialized) {
            viewModel = new ViewModelProvider(this).get(HomeViewModel.class);

            setupBanner();
            setupRecyclerViews();
            setupSwipeRefresh();
            setupSearchBar();

            observeData(viewModel.getLocalProducts());
            triggerRefresh();

            isInitialized = true; // Đánh dấu là đã setup xong
        }
    }

    private void setupRecyclerViews() {
        // 1. SETUP DANH MỤC
        java.util.List<com.example.marketplace.model.Category> categoryList = com.example.marketplace.utils.CategoryHelper.getCategories();

        categoryAdapter = new CategoryAdapter(categoryList);

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

    private void setupBanner() {
        // 1. Tạo danh sách ảnh Banner (Bạn có thể lấy các link ảnh từ Firebase, ở đây tôi giả lập 3 ảnh)
        bannerList = new java.util.ArrayList<>();
        bannerList.add("https://img.freepik.com/premium-vector/online-shopping-concept-with-3d-elements-landing-page_108061-689.jpg");
        bannerList.add("https://img.freepik.com/free-vector/gradient-sale-background_23-2148906371.jpg");
        bannerList.add("https://img.freepik.com/free-vector/flat-sale-banner-with-photo_23-2149026968.jpg");

        BannerAdapter bannerAdapter = new BannerAdapter(bannerList);
        binding.viewPagerBanner.setAdapter(bannerAdapter);

        // Đặt Vị trí bắt đầu ở giữa để có thể vuốt sang trái/phải ngay lập tức
        binding.viewPagerBanner.setCurrentItem(bannerList.size() * 1000, false);

        setupDotsIndicator();

        // 2. Thêm hiệu ứng lật trang (Zoom Out) mượt mà
        binding.viewPagerBanner.setPageTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f); // Hiệu ứng thu nhỏ ảnh hai bên
        });

        binding.viewPagerBanner.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position % bannerList.size());

                // Reset lại bộ đếm thời gian khi người dùng tự vuốt tay
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000); // 3 giây trượt 1 lần
            }
        });
    }

    private final Runnable sliderRunnable = new Runnable() {
        @Override
        public void run() {
            binding.viewPagerBanner.setCurrentItem(binding.viewPagerBanner.getCurrentItem() + 1);
        }
    };

    // Tạo các chấm tròn bên dưới
    private void setupDotsIndicator() {
        android.widget.ImageView[] dots = new android.widget.ImageView[bannerList.size()];
        android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.WRAP_CONTENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(8, 0, 8, 0);

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new android.widget.ImageView(requireContext());
            dots[i].setImageResource(android.R.drawable.presence_invisible); // Dấu chấm xám
            dots[i].setLayoutParams(params);
            binding.layoutDots.addView(dots[i]);
        }
    }

    // Cập nhật chấm tròn sáng lên khi lật trang
    private void updateDots(int currentPosition) {
        int childCount = binding.layoutDots.getChildCount();
        for (int i = 0; i < childCount; i++) {
            android.widget.ImageView imageView = (android.widget.ImageView) binding.layoutDots.getChildAt(i);
            if (i == currentPosition) {
                imageView.setImageResource(android.R.drawable.presence_online); // Dấu chấm sáng (Xanh/Trắng)
            } else {
                imageView.setImageResource(android.R.drawable.presence_invisible); // Dấu chấm xám
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        sliderHandler.removeCallbacks(sliderRunnable); // Tắt động cơ khi thoát app ra nền
    }

    @Override
    public void onResume() {
        super.onResume();
        sliderHandler.postDelayed(sliderRunnable, 3000); // Bật lại động cơ khi vào lại app
    }

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
    }
}