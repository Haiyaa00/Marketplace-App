package com.example.marketplace.ui.post;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
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
import androidx.lifecycle.LiveData;
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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PostFragment extends Fragment {

    private FragmentPostBinding binding;
    private PostViewModel viewModel;
    private UserEntity mCurrentUser;

    // Quản lý ảnh
    private List<Uri> selectedUris = new ArrayList<>();
    private SelectedImageAdapter imageAdapter;

    // Adapter cho Tab Quản lý bài đăng
    private ManagePostAdapter managePostAdapter;

    // ==========================================
    // BIẾN CACHE VIEW (CHỐNG LOAD LẠI) [1]
    // ==========================================
    private View rootView;
    private boolean isInitialized = false;

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
        // CACHE VIEW: Nếu giao diện chưa được tạo thì mới inflate [1]
        if (rootView == null) {
            binding = FragmentPostBinding.inflate(inflater, container, false);
            rootView = binding.getRoot();
        }
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Chỉ setup toàn bộ cấu hình 1 lần duy nhất lúc khởi tạo [1]
        if (!isInitialized) {
            viewModel = new ViewModelProvider(this).get(PostViewModel.class);

            setupDropdownMenu();
            setupAddressDropdowns();
            setupImageRecyclerView();
            observeCurrentUser();
            setupListeners();
            setupTabLayout();
            setupManagePostsRecyclerView();

            isInitialized = true; // Đánh dấu đã khởi tạo thành công [1]
        }
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
            if (user != null) {
                // Chỉ lắng nghe sản phẩm cá nhân đúng 1 lần duy nhất tại đây [1]
                observeMyProducts(user.uid);
            }
        });
    }

    private void observeMyProducts(String userId) {
        viewModel.getMyProducts(userId).observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                managePostAdapter.submitList(products);
            }
        });
    }

    private void setupDropdownMenu() {
        // Lấy danh sách tự động từ Nguồn chân lý duy nhất (Single Source of Truth)
        String[] categories = com.example.marketplace.utils.CategoryHelper.getCategoryNames();
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

        // Định dạng giá tiền tự động khi nhập
        binding.edtPrice.addTextChangedListener(new TextWatcher() {
            private String current = "";

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!s.toString().equals(current)) {
                    binding.edtPrice.removeTextChangedListener(this);

                    String cleanString = s.toString().replaceAll("[^\\d]", "");

                    if (!cleanString.isEmpty()) {
                        try {
                            double parsed = Double.parseDouble(cleanString);

                            DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
                            symbols.setGroupingSeparator('.');
                            DecimalFormat formatter = new DecimalFormat("#,###", symbols);

                            String formatted = formatter.format(parsed);

                            current = formatted;
                            binding.edtPrice.setText(formatted);
                            binding.edtPrice.setSelection(formatted.length());
                        } catch (NumberFormatException e) {
                            e.printStackTrace();
                        }
                    } else {
                        current = "";
                        binding.edtPrice.setText("");
                    }

                    binding.edtPrice.addTextChangedListener(this);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
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

        String fullAddress = street + ", " + ward + ", " + district + ", " + city;

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

        // Loại bỏ dấu phân cách trước khi parse
        double price = Double.parseDouble(priceStr.replaceAll("[^\\d]", ""));
        showLoading(true);

        viewModel.uploadMultipleImagesToCloud(requireContext(), selectedUris).observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
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
        product.setViewCount(0);
        product.setTitle(title);
        product.setPrice(price);
        product.setCategory(category);
        product.setDescription(desc);
        product.setSellerId(mCurrentUser.uid);
        product.setContactPhone(contactPhone);
        product.setAddress(address);
        product.setImageUrls(imageUrls);

        if (imageUrls != null && !imageUrls.isEmpty()) {
            product.setImageUrl(imageUrls.get(0));
        }

        viewModel.createProduct(product).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case SUCCESS:
                    showLoading(false);
                    Toast.makeText(requireContext(), "Đăng bài thành công!", Toast.LENGTH_SHORT).show();
                    resetForm();
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
                if (tab.getPosition() == 0) {
                    binding.layoutCreatePost.setVisibility(View.VISIBLE);
                    binding.layoutManagePosts.setVisibility(View.GONE);
                } else {
                    binding.layoutCreatePost.setVisibility(View.GONE);
                    binding.layoutManagePosts.setVisibility(View.VISIBLE);
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
                Intent intent = new Intent(requireContext(), EditPostActivity.class);
                intent.putExtra("PRODUCT_ID", product.id);
                startActivity(intent);
            }

            @Override
            public void onDelete(ProductEntity product) {
                showDeleteConfirmDialog(product);
            }
            @Override
            public void onDetailClick(ProductEntity product) {
                Intent intent = new Intent(requireContext(), com.example.marketplace.ui.detail.ProductDetailActivity.class);
                intent.putExtra("PRODUCT_ID", product.id);
                startActivity(intent);
            }
        });

        binding.rvMyPosts.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvMyPosts.setAdapter(managePostAdapter);
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

    // Hàm reset form chi tiết [2]
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
    }
}