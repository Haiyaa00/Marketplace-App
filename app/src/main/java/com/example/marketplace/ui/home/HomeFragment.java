package com.example.marketplace.ui.home;

import android.content.Intent;
import android.net.Uri;
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

            viewModel.getBannerUrls().observe(getViewLifecycleOwner(), this::setupBanner);
            setupRecyclerViews();
            setupSwipeRefresh();
            setupSearchBar();
            seedLocalBannersToCloud();

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

    private void setupBanner(List<String> banners) {
        // XÓA BỎ HOÀN TOÀN dòng khai báo "1. Tạo danh sách ảnh Banner" cũ của bạn

        // Gán thẳng danh sách nhận được từ ViewModel vào Adapter
        BannerAdapter bannerAdapter = new BannerAdapter(banners);
        binding.viewPagerBanner.setAdapter(bannerAdapter);

        // Đặt Vị trí bắt đầu ở giữa
        binding.viewPagerBanner.setCurrentItem(banners.size() * 1000, false);

        // Cập nhật lại biến toàn cục bannerList
        this.bannerList = banners;

        setupDotsIndicator();

        // 2. Thêm hiệu ứng lật trang (Giữ nguyên)
        binding.viewPagerBanner.setPageTransformer((page, position) -> {
            float r = 1 - Math.abs(position);
            page.setScaleY(0.85f + r * 0.15f);
        });

        binding.viewPagerBanner.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateDots(position % bannerList.size());
                sliderHandler.removeCallbacks(sliderRunnable);
                sliderHandler.postDelayed(sliderRunnable, 3000);
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
    private int dpToPx(int dp) {
        float density = requireContext().getResources().getDisplayMetrics().density;
        return Math.round((float) dp * density);
    }

    // Tạo các chấm tròn nhỏ mặc định ban đầu (6dp x 6dp)
    private void setupDotsIndicator() {
        binding.layoutDots.removeAllViews(); // Dọn dẹp sạch sẽ các chấm cũ trước khi vẽ
        android.widget.ImageView[] dots = new android.widget.ImageView[bannerList.size()];

        for (int i = 0; i < dots.length; i++) {
            dots[i] = new android.widget.ImageView(requireContext());

            // Đặt kích thước mặc định hình tròn nhỏ 6dp x 6dp
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    dpToPx(6), dpToPx(6));
            params.setMargins(dpToPx(4), 0, dpToPx(4), 0); // Khoảng cách giữa các chấm là 4dp

            dots[i].setLayoutParams(params);
            dots[i].setImageResource(R.drawable.dot_inactive); // Chấm xám mặc định
            binding.layoutDots.addView(dots[i]);
        }
    }

    // Cập nhật chấm tròn sáng lên khi lật trang
    private void updateDots(int currentPosition) {
        int childCount = binding.layoutDots.getChildCount();
        for (int i = 0; i < childCount; i++) {
            android.widget.ImageView imageView = (android.widget.ImageView) binding.layoutDots.getChildAt(i);
            android.widget.LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) imageView.getLayoutParams();

            if (i == currentPosition) {
                // Chấm đang chọn: Chuyển sang màu xanh đại dương và kéo dài ra 16dp tạo hình viên thuốc
                imageView.setImageResource(R.drawable.dot_active);
                params.width = dpToPx(16);
                params.height = dpToPx(6);
            } else {
                // Chấm không chọn: Thu nhỏ lại thành hình tròn xám 6dp x 6dp
                imageView.setImageResource(R.drawable.dot_inactive);
                params.width = dpToPx(6);
                params.height = dpToPx(6);
            }
            imageView.setLayoutParams(params); // Nạp lại cấu hình kích thước mới
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

    private void seedLocalBannersToCloud() {
        String[] localBanners = {"banner1.png", "banner2.png", "banner3.png"};

        for (int i = 0; i < localBanners.length; i++) {
            String fileName = localBanners[i];
            final int index = i; // Lưu lại vị trí để tạo ID cứng

            try {
                // 1. Đọc file từ thư mục assets của APK
                java.io.InputStream is = requireContext().getAssets().open(fileName);
                java.io.File tempFile = new java.io.File(requireContext().getCacheDir(), fileName);
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    fos.write(buffer, 0, read);
                }
                is.close();
                fos.flush();
                fos.close();

                Uri imageUri = Uri.fromFile(tempFile);

                // 2. Đẩy lên Cloudinary
                com.example.marketplace.data.remote.CloudinaryManager.getInstance()
                        .uploadImage(requireContext(), imageUri)
                        .addOnSuccessListener(imageUrl -> {

                            java.util.Map<String, Object> bannerData = new java.util.HashMap<>();
                            bannerData.put("imageUrl", imageUrl);

                            // GIẢI PHÁP CHỐNG TRÙNG LẶP TUYỆT ĐỐI:
                            // Gán cứng ID cho Document (Ví dụ: banner_1, banner_2, banner_3)
                            String documentId = "banner_" + (index + 1);

                            // Dùng .document(id).set() thay vì .add() để luôn GHI ĐÈ, không sinh rác [1]
                            com.example.marketplace.data.remote.FirebaseManager.getInstance().getDb()
                                    .collection("Banners")
                                    .document(documentId) // Đặt ID cố định [1]
                                    .set(bannerData) // Ghi đè dữ liệu cũ [1]
                                    .addOnSuccessListener(aVoid -> {
                                        android.util.Log.d("BannerSeeder", "Đã nạp/ghi đè thành công: " + documentId);
                                    });
                        });

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}