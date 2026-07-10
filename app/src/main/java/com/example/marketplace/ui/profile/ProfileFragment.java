package com.example.marketplace.ui.profile;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.bumptech.glide.Glide;
import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.local.UserEntity;
import com.example.marketplace.databinding.DialogEditProfileBinding;
import com.example.marketplace.databinding.FragmentProfileBinding;
import com.example.marketplace.ui.auth.LoginActivity;
import com.example.marketplace.ui.home.ProductAdapter;
import com.example.marketplace.utils.ImageUtils;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private ProfileViewModel viewModel;
    private ProductAdapter myProductAdapter;
    private UserEntity mCurrentUser;

    // BIẾN CACHE VIEW (CHỐNG LOAD LẠI) [1]
    private View rootView;
    private boolean isInitialized = false;

    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadAndSaveAvatar(uri);
                }
            });

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // CACHE VIEW: Trả về rootView cũ nếu đã được tạo [1]
        if (rootView == null) {
            binding = FragmentProfileBinding.inflate(inflater, container, false);
            rootView = binding.getRoot();
        }
        return rootView;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Chỉ chạy setup 1 lần duy nhất
        if (!isInitialized) {
            viewModel = new ViewModelProvider(this).get(ProfileViewModel.class);

            setupRecyclerView();
            observeUserData();
            setupListeners();

            isInitialized = true; // Đánh dấu đã setup xong
        }
    }

    private void setupRecyclerView() {
        myProductAdapter = new ProductAdapter(false);
        binding.rvMyProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvMyProducts.setAdapter(myProductAdapter);

        myProductAdapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(requireContext(), com.example.marketplace.ui.detail.ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        });
    }

    private void observeUserData() {
        viewModel.getCurrentUser().observe(getViewLifecycleOwner(), user -> {
            if (user != null) {
                mCurrentUser = user;
                binding.tvProfileName.setText(user.name);
                binding.tvProfileEmail.setText(user.email);

                Glide.with(this)
                        .load(user.avatarUrl != null && !user.avatarUrl.isEmpty() ? user.avatarUrl : android.R.drawable.sym_def_app_icon)
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .into(binding.imgAvatar);

                observeFavoriteProducts();
            }
        });
    }

    private void observeFavoriteProducts() {
        viewModel.getFavoriteProducts().observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                myProductAdapter.submitList(products);
            }
        });
    }

    private void setupListeners() {
        binding.btnLogout.setOnClickListener(v -> showLogoutConfirmDialog());

        binding.btnSettings.setOnClickListener(v -> {
            if (mCurrentUser != null) {
                showEditProfileDialog();
            }
        });

        binding.btnHelp.setOnClickListener(v ->
                Toast.makeText(requireContext(), "Mọi phản hồi xin gửi về email hỗ trợ của trường!", Toast.LENGTH_LONG).show()
        );

        binding.btnChangeAvatar.setOnClickListener(v -> {
            if (mCurrentUser != null) {
                pickImageLauncher.launch("image/*");
            }
        });

        binding.imgAvatar.setOnClickListener(v -> {
            if (mCurrentUser != null) {
                showAvatarViewerDialog();
            }
        });
    }

    private void showAvatarViewerDialog() {
        ImageView imageView = new ImageView(requireContext());
        imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        imageView.setPadding(16, 16, 16, 16);

        Glide.with(this)
                .load(mCurrentUser.avatarUrl != null && !mCurrentUser.avatarUrl.isEmpty() ? mCurrentUser.avatarUrl : android.R.drawable.sym_def_app_icon)
                .placeholder(android.R.drawable.sym_def_app_icon)
                .into(imageView);

        new AlertDialog.Builder(requireContext())
                .setView(imageView)
                .setPositiveButton("Đóng", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void uploadAndSaveAvatar(Uri selectedImageUri) {
        Uri compressedUri = ImageUtils.compressImage(requireContext(), selectedImageUri);
        Toast.makeText(requireContext(), "Đang tải ảnh đại diện lên...", Toast.LENGTH_SHORT).show();
        binding.btnChangeAvatar.setEnabled(false);

        viewModel.uploadAvatarToCloud(compressedUri).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case SUCCESS:
                    saveAvatarUrlToDb(resource.data);
                    break;
                case ERROR:
                    binding.btnChangeAvatar.setEnabled(true);
                    Toast.makeText(requireContext(), "Lỗi tải ảnh: " + resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void saveAvatarUrlToDb(String imageUrl) {
        viewModel.updateAvatarUrl(mCurrentUser, imageUrl).observe(getViewLifecycleOwner(), resource -> {
            switch (resource.status) {
                case SUCCESS:
                    binding.btnChangeAvatar.setEnabled(true);
                    Toast.makeText(requireContext(), "Thay ảnh đại diện thành công!", Toast.LENGTH_SHORT).show();
                    break;
                case ERROR:
                    binding.btnChangeAvatar.setEnabled(true);
                    Toast.makeText(requireContext(), "Lỗi lưu dữ liệu: " + resource.message, Toast.LENGTH_SHORT).show();
                    break;
            }
        });
    }

    private void showEditProfileDialog() {
        DialogEditProfileBinding dialogBinding = DialogEditProfileBinding.inflate(getLayoutInflater());
        dialogBinding.edtEditName.setText(mCurrentUser.name);
        dialogBinding.edtEditPhone.setText(mCurrentUser.phone);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setView(dialogBinding.getRoot())
                .setCancelable(false)
                .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        dialogBinding.btnCancelEdit.setOnClickListener(v -> dialog.dismiss());

        dialogBinding.btnSaveEdit.setOnClickListener(v -> {
            String newName = dialogBinding.edtEditName.getText().toString().trim();
            String newPhone = dialogBinding.edtEditPhone.getText().toString().trim();

            if (newName.isEmpty()) {
                dialogBinding.tilEditName.setError("Tên không được để trống");
                return;
            }
            if (newPhone.isEmpty() || newPhone.length() < 9) {
                dialogBinding.tilEditPhone.setError("SĐT không hợp lệ");
                return;
            }

            viewModel.updateProfile(mCurrentUser, newName, newPhone).observe(getViewLifecycleOwner(), resource -> {
                switch (resource.status) {
                    case LOADING:
                        dialogBinding.btnSaveEdit.setEnabled(false);
                        break;
                    case SUCCESS:
                        dialog.dismiss();
                        Toast.makeText(requireContext(), "Cập nhật thông tin thành công!", Toast.LENGTH_SHORT).show();
                        break;
                    case ERROR:
                        dialogBinding.btnSaveEdit.setEnabled(true);
                        Toast.makeText(requireContext(), "Lỗi: " + resource.message, Toast.LENGTH_SHORT).show();
                        break;
                }
            });
        });

        dialog.show();
    }

    private void showDeleteProductDialog(ProductEntity product) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Xóa bài đăng")
                .setMessage("Bạn có chắc chắn muốn xóa sản phẩm:\n\"" + product.title + "\"?\nHành động này không thể hoàn tác.")
                .setPositiveButton("Xóa bài", (dialog, which) -> {
                    viewModel.deleteProduct(product.id).observe(getViewLifecycleOwner(), resource -> {
                        switch (resource.status) {
                            case SUCCESS:
                                Toast.makeText(requireContext(), "Đã xóa bài đăng thành công!", Toast.LENGTH_SHORT).show();
                                break;
                            case ERROR:
                                Toast.makeText(requireContext(), "Lỗi khi xóa: " + resource.message, Toast.LENGTH_SHORT).show();
                                break;
                        }
                    });
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showLogoutConfirmDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Đăng xuất")
                .setMessage("Bạn có chắc chắn muốn đăng xuất tài khoản khỏi thiết bị?")
                .setPositiveButton("Đăng xuất", (dialog, which) -> {
                    viewModel.logout();
                    Toast.makeText(requireContext(), "Đã đăng xuất thành công!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(requireContext(), LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    requireActivity().finish();
                })
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss())
                .show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }
}