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
        setupImageRecyclerView();
        observeCurrentUser();
        setupListeners();
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
                saveProductToDatabase(title, price, category, description, contactPhone, imageUrls);
            } else if (resource.status == Resource.Status.ERROR) {
                showLoading(false);
                Toast.makeText(requireContext(), resource.message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProductToDatabase(String title, double price, String category, String desc, String contactPhone, List<String> imageUrls) {
        if (mCurrentUser == null) return;

        Product product = new Product();
        product.setTitle(title);
        product.setPrice(price);
        product.setCategory(category);
        product.setDescription(desc);
        product.setSellerId(mCurrentUser.uid);
        product.setContactPhone(contactPhone);
        product.setImageUrls(imageUrls); // Set danh sách link ảnh
        product.setTimestamp(System.currentTimeMillis());

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