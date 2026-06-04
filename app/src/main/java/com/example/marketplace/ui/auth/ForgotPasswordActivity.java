package com.example.marketplace.ui.auth;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.marketplace.databinding.ActivityForgotPasswordBinding;
import com.example.marketplace.utils.ValidatorUtils;

public class ForgotPasswordActivity extends AppCompatActivity {

    private ActivityForgotPasswordBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        setupListeners();
    }

    private void setupListeners() {
        binding.btnBack.setOnClickListener(v -> finish());

        binding.btnSend.setOnClickListener(v -> attemptSendResetEmail());
    }

    private void attemptSendResetEmail() {
        String email = binding.edtEmail.getText().toString().trim();

        if (email.isEmpty()) {
            binding.tilEmail.setError("Vui lòng nhập email");
            return;
        }

        if (!ValidatorUtils.isValidEduEmail(email)) {
            binding.tilEmail.setError("Chỉ chấp nhận email sinh viên @kientruchanoi.edu.vn");
            return;
        }

        binding.tilEmail.setError(null);
        showLoading(true);

        viewModel.sendPasswordResetEmail(email).observe(this, resource -> {
            showLoading(false);
            switch (resource.status) {
                case SUCCESS:
                    Toast.makeText(this, "Yêu cầu đặt lại mật khẩu đã được gửi đến email của bạn!", Toast.LENGTH_LONG).show();
                    finish();
                    break;
                case ERROR:
                    Toast.makeText(this, "Lỗi: " + resource.message, Toast.LENGTH_LONG).show();
                    break;
            }
        });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.progressBar.setVisibility(View.VISIBLE);
            binding.btnSend.setText("");
            binding.btnSend.setEnabled(false);
        } else {
            binding.progressBar.setVisibility(View.GONE);
            binding.btnSend.setText("Gửi yêu cầu");
            binding.btnSend.setEnabled(true);
        }
    }
}
