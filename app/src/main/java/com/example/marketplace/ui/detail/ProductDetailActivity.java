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
    private String currentProductId;
    private boolean isCurrentlyFavorite = false;
    private String contactPhone = ""; // Lưu tạm số điện thoại để gọi
    private String sellerId = "";
    private String sellerName ="";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityProductDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(DetailViewModel.class);

        String productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId != null) {
            currentProductId = productId; // Lưu ID
            loadProductData(productId);
            checkFavoriteStatus(); // Gọi hàm observe trạng thái yêu thích
        } else {
            Toast.makeText(this, "Lỗi: Không tìm thấy sản phẩm!", Toast.LENGTH_SHORT).show();
            finish();
        }

        // ================= CÀI ĐẶT SỰ KIỆN CLICK =================
        // Nút Back trên thanh Top Bar
        binding.btnBackTop.setOnClickListener(v -> finish());

        // Nút Yêu thích trên thanh Top Bar
        binding.btnFavoriteTop.setOnClickListener(v -> {
            if (currentProductId != null) {
                viewModel.toggleFavorite(currentProductId, isCurrentlyFavorite);
            }
        });

        // Nút Yêu thích lớn ở trong thẻ Thông tin
        binding.btnFavoriteMain.setOnClickListener(v -> {
            if (currentProductId != null) {
                viewModel.toggleFavorite(currentProductId, isCurrentlyFavorite);
            }
        });

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
                binding.tvDate.setText(com.example.marketplace.utils.DateUtils.getRelativeTimeSpan(product.timestamp));
                binding.tvAddress.setText(product.address);

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

    private void loadSellerInfo(String sId) {
        this.sellerId = sId;
        viewModel.getSellerInfo(sId).observe(this, seller -> {
            if (seller != null) {
                this.sellerName = seller.getName();
                binding.tvSellerName.setText(sellerName);

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
        binding.btnChat.setText("Chat Ngay");
        binding.btnChat.setOnClickListener(v -> {
            // Không cho phép tự chat với chính mình
            String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (currentUserId.equals(sellerId)) {
                Toast.makeText(this, "Đây là sản phẩm của bạn!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!sellerId.isEmpty()) {
                Intent intent = new Intent(this, com.example.marketplace.ui.chat.ChatActivity.class);
                intent.putExtra("PARTNER_ID", sellerId);
                intent.putExtra("PARTNER_NAME", sellerName);
                startActivity(intent);
            }
        });
    }

    private void checkFavoriteStatus() {
        if (currentProductId == null) return;

        viewModel.isFavorite(currentProductId).observe(this, isFav -> {
            isCurrentlyFavorite = isFav != null && isFav;

            // Đổi icon cho CẢ 2 NÚT cùng 1 lúc để giao diện đồng bộ
            if (isCurrentlyFavorite) {
                // Đang yêu thích -> Chuyển thành icon Bật (Màu đỏ/Sáng)
                binding.btnFavoriteTop.setImageResource(android.R.drawable.btn_star_big_on);
                binding.btnFavoriteMain.setImageResource(android.R.drawable.btn_star_big_on);
            } else {
                // Không yêu thích -> Chuyển thành icon Tắt (Màu xám/Tối)
                binding.btnFavoriteTop.setImageResource(android.R.drawable.btn_star_big_off);
                binding.btnFavoriteMain.setImageResource(android.R.drawable.btn_star_big_off);
            }
        });
    }
}