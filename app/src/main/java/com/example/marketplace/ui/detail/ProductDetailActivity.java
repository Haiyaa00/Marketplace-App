package com.example.marketplace.ui.detail;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.viewpager2.widget.ViewPager2;

import com.bumptech.glide.Glide;
import com.example.marketplace.databinding.ActivityProductDetailBinding;
import com.example.marketplace.model.User;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private ActivityProductDetailBinding binding;
    private DetailViewModel viewModel;
    private String contactPhone = ""; // Lưu tạm số điện thoại để gọi/zalo

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);

        // 1. Nhận ID sản phẩm từ Intent
        String productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId != null) {
            loadProductData(productId);
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy sản phẩm!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // 2. Nút Quay lại
        binding.btnBack.setOnClickListener(v -> finish());

        // 3. Nút Gọi điện và Zalo
        setupContactButtons();
    }

    private void loadProductData(String productId) {
        viewModel.getProductDetails(productId).observe(this, product -> {
            if (product != null) {
                // Lưu số điện thoại để dùng cho nút bấm
                contactPhone = product.contactPhone;

                // Set Text
                binding.tvTitle.setText(product.title);
                binding.tvCategory.setText(product.category);
                binding.tvDescription.setText(product.description);

                DecimalFormat formatter = new DecimalFormat("#,###");
                binding.tvPrice.setText(formatter.format(product.price) + " đ");

                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", new Locale("vi", "VN"));
                binding.tvDate.setText(sdf.format(new Date(product.timestamp)));

                // Set Ảnh Slider (ViewPager2)
                setupImageSlider(product.imageUrls);

                // Lấy thông tin người bán
                loadSellerInfo(product.sellerId);
            }
        });
    }

    private void setupImageSlider(List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            ImageSliderAdapter adapter = new ImageSliderAdapter(imageUrls);
            binding.viewPagerImages.setAdapter(adapter);

            // Cập nhật bộ đếm ảnh khi lướt (Ví dụ: 2/5)
            binding.tvImageCounter.setText("1/" + imageUrls.size());
            binding.viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    binding.tvImageCounter.setText((position + 1) + "/" + imageUrls.size());
                }
            });
        }
    }

    private void loadSellerInfo(String sellerId) {
        viewModel.getSellerInfo(sellerId).observe(this, seller -> {
            if (seller != null) {
                binding.tvSellerName.setText(seller.getName());

                Glide.with(this)
                        .load(seller.getAvatarUrl() != null && !seller.getAvatarUrl().isEmpty() ? seller.getAvatarUrl() : android.R.drawable.sym_def_app_icon)
                        .placeholder(android.R.drawable.sym_def_app_icon)
                        .into(binding.ivSellerAvatar);
            }
        });
    }

    private void setupContactButtons() {
        // NÚT GỌI ĐIỆN (Mở app gọi điện của máy)
        binding.btnCall.setOnClickListener(v -> {
            if (contactPhone != null && !contactPhone.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + contactPhone));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Không có số điện thoại!", Toast.LENGTH_SHORT).show();
            }
        });

        // NÚT CHAT ZALO (Mở app Zalo qua Deeplink)
        binding.btnZalo.setOnClickListener(v -> {
            if (contactPhone != null && !contactPhone.isEmpty()) {
                // Định dạng số điện thoại chuẩn Zalo (Bỏ số 0 đầu, thay bằng 84)
                String zaloPhone = contactPhone;
                if (zaloPhone.startsWith("0")) {
                    zaloPhone = "84" + zaloPhone.substring(1);
                }

                String url = "https://zalo.me/" + zaloPhone;
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse(url));
                startActivity(intent);
            } else {
                Toast.makeText(this, "Không có số điện thoại!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}