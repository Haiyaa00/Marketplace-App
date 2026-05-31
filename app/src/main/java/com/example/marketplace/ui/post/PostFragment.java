package com.example.marketplace.ui.post;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.R;
import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.local.UserEntity;
import com.example.marketplace.databinding.FragmentPostBinding;
import com.example.marketplace.model.Product;
import com.example.marketplace.utils.AddressParser;
import com.example.marketplace.utils.Resource;

import java.util.ArrayList;
import java.util.List;

public class PostFragment extends Fragment {

    private FragmentPostBinding binding;
    private PostViewModel viewModel;
    private UserEntity mCurrentUser;

    // Quản lý ảnh
    private List<Uri> selectedUris = new ArrayList<>();
    private SelectedImageAdapter imageAdapter;

    // Adapter cho Tab Quản lý bài đăng
    private ManagePostAdapter managePostAdapter;

    // Bộ chọn nhiều ảnh từ Gallery
    private final ActivityResultLauncher<String> pickMultipleImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (selectedUris.size() < 10) {
                            selectedUris.add(uri);
                        } else {
                            Toast.makeText(requireContext(), "Chỉ được chọn tối đa 10 ảnh!", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                    imageAdapter.notifyDataSetChanged();
                    updateImageCount();
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentPostBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(PostViewModel.class);

        setupDropdownMenu();
        setupAddressDropdowns();
        setupImageRecyclerView();
        observeCurrentUser();
        setupListeners();
        setupTabLayout();
        setupManagePostsRecyclerView();
    }

    private void setupAddressDropdowns() {
        AddressParser.loadHanoiData(requireContext());

        List<String> districts = AddressParser.getDistricts();
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, districts);
        binding.actDistrict.setAdapter(districtAdapter);

        binding.actDistrict.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDistrict = (String) parent.getItemAtPosition(position);
            binding.actWard.setText("", false);

            List<String> wards = AddressParser.getWards(selectedDistrict);
            ArrayAdapter<String> wardAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, wards);
            binding.actWard.setAdapter(wardAdapter);
        });
    }

    private void observeCurrentUser() {
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            mCurrentUser = user;
            // Tự động điền số điện thoại
            if (user != null && binding.edtContactPhone.getText().toString().isEmpty()) {
                binding.edtContactPhone.setText(user.phone);
            }
        });
    }

    private void setupDropdownMenu() {
        String[] categories = new String[]{"Giáo trình", "Phòng trọ", "Điện tử", "Đồ gia dụng", "Khác"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, categories);
        binding.actCategory.setAdapter(adapter);
    }

    private void setupImageRecyclerView() {
        imageAdapter = new SelectedImageAdapter(selectedUris, this::updateImageCount);
        binding.rvSelectedImages.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
        binding.rvSelectedImages.setAdapter(imageAdapter);

        binding.btnAddImages.setOnClickListener(v -> {
            if (selectedUris.size() >= 10) {
                Toast.makeText(requireContext(), "Đã đạt tối đa 10 ảnh!", Toast.LENGTH_SHORT).show();
            } else {
                pickMultipleImagesLauncher.launch("image/*");
            }
        });
    }

    private void updateImageCount() {
        binding.tvImageCount.setText(selectedUris.size() + "/10 ảnh");
    }

    private void setupListeners() {
        binding.btnPost.setOnClickListener(v -> attemptPostProduct());
    }

    private void attemptPostProduct() {
        String title = binding.edtTitle.getText().toString().trim();
        String priceStr = binding.edtPrice.getText().toString().trim();
        String category = binding.actCategory.getText().toString().trim();
        String description = binding.edtDescription.getText().toString().trim();
        String contactPhone = binding.edtContactPhone.getText().toString().trim();

        // Thu thập địa chỉ chi tiết
        String street = binding.edtStreet.getText().toString().trim();
        String ward = binding.actWard.getText().toString().trim();
        String district = binding.actDistrict.getText().toString().trim();
        String city = binding.edtCity.getText().toString().trim();

        if (street.isEmpty() || ward.isEmpty() || district.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng nhập đầy đủ địa chỉ!", Toast.LENGTH_SHORT).show();
            return;
        }

        // Tạo 1 string địa chỉ hoàn chỉnh
        String fullAddress = street + ", " + ward + ", " + district + ", " + city;

        // Bắt lỗi Validation
        if (selectedUris.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng chọn ít nhất 1 ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (title.isEmpty() || priceStr.isEmpty() || category.isEmpty() || description.isEmpty() || contactPhone.isEmpty()) {
            Toast.makeText(requireContext(), "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (contactPhone.length() < 9) {
            binding.tilContactPhone.setError("Số điện thoại không hợp lệ");
            return;
        } else {
            binding.tilContactPhone.setError(null);
        }

        double price = Double.parseDouble(priceStr);
        showLoading(true);

        // Upload ảnh lên mạng trước (Do màn này giờ chỉ là màn TẠO MỚI)
        viewModel.uploadMultipleImagesToCloud(requireContext(), selectedUris).observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                // Thứ tự tham số chuẩn: Tên, Giá, Danh mục, Mô tả, SĐT, Địa chỉ, Danh sách link ảnh
                saveProductToDatabase(title, price, category, description, contactPhone, fullAddress, resource.data);
            } else if (resource.status == Resource.Status.ERROR) {
                showLoading(false);
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProductToDatabase(String title, double price, String category, String desc, String contactPhone, String address, List<String> imageUrls) {
        if (mCurrentUser == null) return;

        Product product = new Product();
        product.setTimestamp(System.currentTimeMillis());
        product.setViewCount(0); // Lượt xem ban đầu luôn = 0
        product.setTitle(title);
        product.setPrice(price);
        product.setCategory(category);
        product.setDescription(desc);
        product.setSellerId(mCurrentUser.uid);
        product.setContactPhone(contactPhone);
        product.setAddress(address);
        product.setImageUrls(imageUrls);

        // Set ảnh đầu tiên làm ảnh đại diện phụ (cho các class cũ chưa kịp migrate sang imageUrls)
        if (imageUrls != null && !imageUrls.isEmpty()) {
            product.setImageUrl(imageUrls.get(0));
        }

        viewModel.createProduct(product).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case SUCCESS:
                    showLoading(false);
                    Toast.makeText(requireContext(), "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                    resetForm();
                    // Đẩy về Trang chủ
                    Navigation.findNavController(requireView()).navigate(R.id.nav_home);
                    break;

                case ERROR:
                    showLoading(false);
                    Toast.makeText(requireContext(), "Lỗi: " + resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void setupTabLayout() {
        binding.tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                if (tab.getPosition() == 0) { // Tab Đăng Bán
                    binding.layoutCreatePost.setVisibility(View.VISIBLE);
                    binding.layoutManagePosts.setVisibility(View.GONE);
                } else { // Tab Quản Lý
                    binding.layoutCreatePost.setVisibility(View.GONE);
                    binding.layoutManagePosts.setVisibility(View.VISIBLE);
                    // Load danh sách bài đăng của User
                    if (mCurrentUser != null) {
                        loadMyPosts();
                    }
                }
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });
    }

    private void setupManagePostsRecyclerView() {
        managePostAdapter = new ManagePostAdapter(new ManagePostAdapter.OnPostActionListener() {
            @Override
            public void onEdit(ProductEntity product) {
                // CHUYỂN SANG MÀN HÌNH EDIT MỚI HOÀN TOÀN
                android.content.Intent intent = new android.content.Intent(requireContext(), EditPostActivity.class);
                intent.putExtra("PRODUCT_ID", product.id);
                startActivity(intent);
            }

            @Override
            public void onDelete(ProductEntity product) {
                showDeleteConfirmDialog(product);
            }
        });

        binding.rvMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMyPosts.setAdapter(managePostAdapter);
    }

    private void loadMyPosts() {
        viewModel.getMyProducts(mCurrentUser.uid).observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                managePostAdapter.submitList(products);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnPost.setText("");
            binding.btnPost.setEnabled(false);
            binding.btnAddImages.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnPost.setText("Đăng Bán Ngay");
            binding.btnPost.setEnabled(true);
            binding.btnAddImages.setEnabled(true);
        }
    }

    private void resetForm() {
        binding.edtTitle.setText("");
        binding.edtPrice.setText("");
        binding.actCategory.setText("", false);
        binding.edtDescription.setText("");
        binding.edtContactPhone.setText(mCurrentUser != null ? mCurrentUser.phone : "");

        binding.actDistrict.setText("", false);
        binding.actWard.setText("", false);
        binding.edtStreet.setText("");

        binding.tvImageCount.setText("0/10 ảnh");

        selectedUris.clear();
        imageAdapter.notifyDataSetChanged();
    }

    private void showDeleteConfirmDialog(ProductEntity product) {
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("Xóa bài đăng")
                .setMessage("Bạn có chắc muốn xóa: " + product.title + "?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    viewModel.deleteProduct(product.id).observe(getViewLifecycleOwner(), res -> {
                        if (res.status == Resource.Status.SUCCESS) {
                            Toast.makeText(requireContext(), "Đã xóa", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}