package com.example.marketplace.ui.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.MainActivity;
import com.example.marketplace.databinding.ActivityLoginBinding;
import com.example.marketplace.utils.ValidatorUtils;
import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null && auth.getCurrentUser().isEmailVerified()) {
            // Nếu Firebase ghi nhận đã đăng nhập + email đã click link xác thực
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish(); // Đóng LoginActivity ngay lập tức để người dùng không bấm BACK quay lại được
            return;   // DỪNG thực hiện các dòng lệnh bên dưới (Không khởi tạo giao diện Login)
        }
        // ============================================================================

        // Nếu chưa đăng nhập, khởi tạo giao diện Login như bình thường
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();

        // 1. Khởi tạo ViewBinding
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 2. Khởi tạo ViewModel
        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // 3. Xử lý các nút bấm
        setupListeners();
    }

    private void setupListeners() {
        // Nút Đăng nhập
        binding.btnLogin.setOnClickListener(v -> attemptLogin());

        // Chuyển sang màn hình Đăng ký
        binding.tvGoToRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            // Không gọi finish() để người dùng có thể bấm phím Back quay lại Login
        });
    }

    private void attemptLogin() {
        String email = binding.edtEmail.getText().toString().trim();
        String password = binding.edtPassword.getText().toString().trim();

        // Xóa lỗi cũ
        binding.tilEmail.setError(null);
        binding.tilPassword.setError(null);

        // Validation (Kiểm tra lỗi)
        if (email.isEmpty()) {
            binding.tilEmail.setError("Vui lòng nhập email");
            return;
        }

        // Gọi helper mà ta đã viết ở Bước 3
        if (!ValidatorUtils.isValidEduEmail(email)) {
            binding.tilEmail.setError("Chỉ chấp nhận email sinh viên @kientruchanoi.edu.vn");
            return;
        }

        if (password.isEmpty() || password.length() < 6) {
            binding.tilPassword.setError("Mật khẩu phải có ít nhất 6 ký tự");
            return;
        }

        // Gọi ViewModel và Lắng nghe trạng thái (Observe)
        viewModel.login(email, password).observe(this, resource -> {
            switch (resource.status) {
                case LOADING:
                    showLoading(true);
                    break;

                case SUCCESS:
                    showLoading(false);
                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
                    // Chuyển sang màn hình chính và xóa sạch back-stack (không cho back lại màn login)
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
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
            binding.btnLogin.setText(""); // Giấu chữ nút đi khi đang xoay
            binding.btnLogin.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnLogin.setText("Đăng Nhập");
            binding.btnLogin.setEnabled(true);
        }
    }
}