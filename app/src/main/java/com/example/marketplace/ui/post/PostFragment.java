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
import com.example.marketplace.data.local.UserEntity;
import com.example.marketplace.databinding.FragmentPostBinding;
import com.example.marketplace.model.Product;
import com.example.marketplace.ui.home.ProductAdapter;
import com.example.marketplace.utils.AddressParser;
import com.example.marketplace.utils.Resource;

import java.util.ArrayList;
import java.util.List;

public class PostFragment extends Fragment {

    private FragmentPostBinding binding;
    private PostViewModel viewModel;
    private UserEntity mCurrentUser;

    // Quản lý nhiều ảnh
    private List<Uri> selectedUris = new ArrayList<>();
    private SelectedImageAdapter imageAdapter;
    private ProductAdapter myPostsAdapter;

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
        // Load data từ JSON
        AddressParser.loadHanoiData(requireContext());

        // Đổ danh sách Quận ra Dropdown
        List<String> districts = AddressParser.getDistricts();
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, districts);
        binding.actDistrict.setAdapter(districtAdapter);

        // Sự kiện: Khi chọn Quận -> Load Phường tương ứng
        binding.actDistrict.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDistrict = (String) parent.getItemAtPosition(position);
            
            // Xóa dữ liệu Phường cũ
            binding.actWard.setText("", false);
            
            // Đổ danh sách Phường mới
            List<String> wards = AddressParser.getWards(selectedDistrict);
            ArrayAdapter<String> wardAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_dropdown_item_1line, wards);
            binding.actWard.setAdapter(wardAdapter);
        });
    }

    private void observeCurrentUser() {
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            mCurrentUser = user;
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

        // Bấm nút thêm ảnh
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

        double price = Double.parseDouble(priceStr);

        showLoading(true);

        viewModel.uploadMultipleImagesToCloud(requireContext(), selectedUris).observe(getViewLifecycleOwner(), resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                List<String> imageUrls = resource.data;
                saveProductToDatabase(title, price, category, description, contactPhone, fullAddress, imageUrls);
            } else if (resource.status == Resource.Status.ERROR) {
                showLoading(false);
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProductToDatabase(String title, double price, String category, String desc, String contactPhone, String address, List<String> imageUrls) {
        if (mCurrentUser == null) return;

        Product product = new Product();
        product.setTitle(title);
        product.setPrice(price);
        product.setCategory(category);
        product.setDescription(desc);
        product.setSellerId(mCurrentUser.uid);
        product.setContactPhone(contactPhone);
        product.setImageUrls(imageUrls); // Set danh sách link ảnh
        if (imageUrls != null && !imageUrls.isEmpty()) {
            product.setImageUrl(imageUrls.get(0)); // Set ảnh đại diện
        }
        product.setTimestamp(System.currentTimeMillis());
        product.setAddress(address); // Gán địa chỉ vào đối tượng sản phẩm

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
                    Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
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
        // Truyền "true" để hiển thị nút Xóa (Thùng rác đỏ)
        myPostsAdapter = new ProductAdapter(true);
        binding.rvMyPosts.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(requireContext(), 2));
        binding.rvMyPosts.setAdapter(myPostsAdapter);

        // Sự kiện xóa bài
        myPostsAdapter.setOnDeleteClickListener(product -> {
            // Hiển thị Dialog xóa tương tự bên ProfileFragment (Bạn có thể copy sang)
            // viewModel.deleteProduct(product.id) ...
        });
    }

    private void loadMyPosts() {
        viewModel.getMyProducts(mCurrentUser.uid).observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                myPostsAdapter.submitList(products);
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnPost.setText("");
            binding.btnPost.setEnabled(false);
            binding.btnAddImages.setEnabled(false); // Khóa nút thêm ảnh khi đang upload
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
        binding.actCategory.setText("");
        binding.edtDescription.setText("");
        binding.edtContactPhone.setText("");
        
        binding.actDistrict.setText("", false);
        binding.actWard.setText("", false);
        binding.edtStreet.setText("");

        selectedUris.clear();
        imageAdapter.notifyDataSetChanged();
        updateImageCount();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
