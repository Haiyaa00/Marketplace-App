package com.example.marketplace.ui.auth;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.databinding.ActivityRegisterBinding;
import com.example.marketplace.databinding.DialogVerificationBinding;
import com.example.marketplace.model.User;
import com.example.marketplace.utils.ValidatorUtils;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private AuthViewModel viewModel;

    // Khai báo các biến cho Dialog Động
    private AlertDialog verificationDialog;
    private DialogVerificationBinding dialogBinding;
    private Handler pollingHandler;
    private Runnable pollingRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Khởi tạo Handler gắn với Main Thread (UI Thread)
        pollingHandler = new Handler(Looper.getMainLooper());

        setupListeners();
    }

    private void setupListeners() {
        binding.btnRegister.setOnClickListener(v -> attemptRegister());
        binding.tvGoToLogin.setOnClickListener(v -> finish());
    }

    private void attemptRegister() {
        String name = binding.edtName.getText().toString().trim();
        String phone = binding.edtPhone.getText().toString().trim();
        String email = binding.edtEmail.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();
        String confirmPassword = binding.edtConfirmPassword.getText().toString().trim();

        // (Phần validation giữ nguyên như cũ)
        binding.tilName.setError(null); binding.tilPhone.setError(null);
        binding.tilEmail.setError(null); binding.tilPassword.setError(null);
        binding.tilConfirmPassword.setError(null);

        if (name.isEmpty()) { binding.tilName.setError("Vui lòng nhập họ tên"); return; }
        if (phone.isEmpty() || phone.length() < 9) { binding.tilPhone.setError("Vui lòng nhập SĐT hợp lệ"); return; }
        if (!ValidatorUtils.isValidEduEmail(email)) { binding.tilEmail.setError("Chỉ chấp nhận email @edu.vn"); return; }
        if (password.length() < 6) { binding.tilPassword.setError("Mật khẩu phải từ 6 ký tự trở lên"); return; }
        if (!password.equals(confirmPassword)) { binding.tilConfirmPassword.setError("Mật khẩu xác nhận không khớp"); return; }

        User newUser = new User();
        newUser.setName(name);
        newUser.setPhone(phone);
        newUser.setEmail(email);
        newUser.setAvatarUrl("");

        viewModel.register(newUser, password).observe(this, resource -> {
            switch (resource.status) {
                case LOADING:
                    showLoading(true);
                    break;
                case SUCCESS:
                    showLoading(false);
                    showDynamicVerificationDialog(email);
                    break;
                case ERROR:
                    showLoading(false);
                    Toast.makeText(this, "Lỗi: " + resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnRegister.setText("");
            binding.btnRegister.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnRegister.setText("Đăng Ký");
            binding.btnRegister.setEnabled(true);
        }
    }

    // ================= TÍNH NĂNG DIALOG ĐỘNG =================

    private void showDynamicVerificationDialog(String email) {
        // 1. Khởi tạo ViewBinding cho Dialog
        dialogBinding = com.example.marketplace.databinding.DialogVerificationBinding.inflate(getLayoutInflater());

        // 2. Cập nhật Text email cho thông báo
        dialogBinding.tvDialogMessage.setText("Một email xác thực đã được gửi tới:\n" + email + "\n\nVui lòng kiểm tra hộp thư đến (hoặc Spam). Màn hình này sẽ tự động chuyển đổi khi bạn hoàn tất...");

        // 3. Xây dựng Custom Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Material_Light_Dialog_Alert);
        builder.setView(dialogBinding.getRoot());
        builder.setCancelable(false); // Bắt buộc user phải thao tác qua nút bấm

        verificationDialog = builder.create();

        // Xóa background mặc định của Android Dialog để lấy corner radius của MaterialCardView
        if (verificationDialog.getWindow() != null) {
            verificationDialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }

        // 4. Xử lý nút Hủy
        dialogBinding.btnDialogCancel.setOnClickListener(v -> {
            stopPolling();
            viewModel.logout();
            verificationDialog.dismiss();
            finish(); // Quay lại màn đăng nhập
        });

        // 5. Hiển thị Dialog và Bắt đầu Polling
        verificationDialog.show();
        startPolling();
    }

    private void startPolling() {
        pollingRunnable = new Runnable() {
            @Override
            public void run() {
                viewModel.checkVerificationStatus().observe(RegisterActivity.this, isVerified -> {
                    if (isVerified) {
                        // NẾU ĐÃ XÁC THỰC -> Dừng vòng lặp và Đổi giao diện Custom Dialog
                        stopPolling();
                        updateDialogToSuccess();
                    } else {
                        // Tiếp tục gọi lại sau 3 giây
                        pollingHandler.postDelayed(this, 3000);
                    }
                });
            }
        };
        pollingHandler.post(pollingRunnable);
    }

    private void updateDialogToSuccess() {
        if (verificationDialog != null && verificationDialog.isShowing() && dialogBinding != null) {

            // 1. Đổi Icon thành dấu Tick thành công
            dialogBinding.ivDialogIcon.setImageResource(android.R.drawable.ic_dialog_info); // Có thể thay bằng icon tick xanh của bạn

            // 2. Đổi Text
            dialogBinding.tvDialogTitle.setText("🎉 Chúc mừng!");
            dialogBinding.tvDialogMessage.setText("Tài khoản của bạn đã được xác thực thành công. Bây giờ bạn có thể đăng nhập vào ứng dụng.");

            // 3. Ẩn ProgressBar đang xoay đi
            dialogBinding.pbDialogLoading.setVisibility(View.GONE);

            // 4. Đổi Nút bấm
            dialogBinding.btnDialogCancel.setVisibility(View.GONE); // Ẩn nút hủy
            dialogBinding.btnDialogLogin.setVisibility(View.VISIBLE); // Hiện nút Đăng nhập

            // 5. Sự kiện khi bấm nút Đăng nhập
            dialogBinding.btnDialogLogin.setOnClickListener(v -> {
                viewModel.logout(); // Đăng xuất session ảo
                verificationDialog.dismiss();
                finish(); // Quay về LoginActivity
            });
        }
    }

    private void stopPolling() {
        if (pollingHandler != null && pollingRunnable != null) {
            pollingHandler.removeCallbacks(pollingRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopPolling();
        if (verificationDialog != null && verificationDialog.isShowing()) {
            verificationDialog.dismiss();
        }
    }
}