package com.example.marketplace.ui.post;

import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback; // Import class mới xử lý nút Back
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.databinding.ActivityEditPostBinding;
import com.example.marketplace.model.Product;
import com.example.marketplace.utils.AddressParser;
import com.example.marketplace.utils.Resource;

import java.util.ArrayList;
import java.util.List;

public class EditPostActivity extends AppCompatActivity {

    private ActivityEditPostBinding binding;
    private PostViewModel viewModel;

    private ProductEntity currentProduct; // Sản phẩm đang sửa

    private List<Uri> mixedImageUris = new ArrayList<>(); // Chứa cả ảnh cũ (http) và mới (content)
    private SelectedImageAdapter imageAdapter;

    // Trình chọn nhiều ảnh
    private final ActivityResultLauncher<String> pickMultipleImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.GetMultipleContents(), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    for (Uri uri : uris) {
                        if (mixedImageUris.size() < 10) {
                            mixedImageUris.add(uri);
                        } else {
                            Toast.makeText(this, "Tối đa 10 ảnh!", Toast.LENGTH_SHORT).show();
                            break;
                        }
                    }
                    imageAdapter.notifyDataSetChanged();
                    updateImageCount();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditPostBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(PostViewModel.class);

        // ĐĂNG KÝ SỰ KIỆN NÚT BACK (CHUẨN ANDROID 13+)
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                showExitDialog(); // Gọi hàm hiển thị Dialog khi người dùng vuốt quay lại
            }
        });

        setupDropdownMenu();
        setupAddressDropdowns();
        setupImageRecyclerView();
        setupListeners();

        // Nhận ID từ Tab Quản lý truyền sang
        String productId = getIntent().getStringExtra("PRODUCT_ID");
        if (productId != null) {
            loadProductData(productId);
        } else {
            Toast.makeText(this, "Lỗi tải bài đăng!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadProductData(String productId) {
        viewModel.getProductById(productId).observe(this, product -> {
            if (product != null && currentProduct == null) {
                currentProduct = product;

                // 1. Fill Text Fields (Thông tin cơ bản)
                binding.edtTitle.setText(product.title);
                binding.edtPrice.setText(String.valueOf(product.price));
                binding.edtDescription.setText(product.description);
                binding.edtContactPhone.setText(product.contactPhone);
                binding.actCategory.setText(product.category, false);

                // ================================================================
                // 2. LOGIC BÓC TÁCH & HIỂN THỊ ĐỊA CHỈ THÔNG MINH
                // ================================================================
                if (product.address != null && !product.address.isEmpty()) {
                    try {
                        // Tách chuỗi địa chỉ dựa theo dấu phẩy
                        String[] addressParts = product.address.split(",");

                        // Đảm bảo mảng tách ra có đủ ít nhất 3 phần (Số nhà, Phường, Quận)
                        if (addressParts.length >= 3) {
                            // Dùng trim() để xóa các khoảng trắng dư thừa ở đầu và cuối chữ
                            String street = addressParts[0].trim();
                            String ward = addressParts[1].trim();
                            String district = addressParts[2].trim();

                            // Set text cho Số nhà
                            binding.edtStreet.setText(street);

                            // Set text cho Quận/Huyện (false để không xổ dropdown ra ngay lập tức)
                            binding.actDistrict.setText(district, false);

                            // Set text cho Phường/Xã
                            binding.actWard.setText(ward, false);

                            // MA THUẬT UX: Phải nạp lại danh sách Phường cho đúng Quận này
                            // Để nếu người dùng muốn đổi Phường khác, bấm vào nó vẫn xổ ra đúng list!
                            java.util.List<String> wardsList = com.example.marketplace.utils.AddressParser.getWards(district);
                            android.widget.ArrayAdapter<String> wardAdapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, wardsList);
                            binding.actWard.setAdapter(wardAdapter);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // ================================================================
                // 3. Load ẢNH CŨ vào danh sách
                // ================================================================
                if (product.imageUrls != null) {
                    for (String url : product.imageUrls) {
                        mixedImageUris.add(Uri.parse(url)); // Uri.parse biến chuỗi web thành Uri
                    }
                    imageAdapter.notifyDataSetChanged();
                    updateImageCount();
                }
            }
        });
    }

    private void setupImageRecyclerView() {
        imageAdapter = new SelectedImageAdapter(mixedImageUris, this::updateImageCount);
        binding.rvSelectedImages.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        binding.rvSelectedImages.setAdapter(imageAdapter);

        binding.btnAddImages.setOnClickListener(v -> {
            if (mixedImageUris.size() >= 10) {
                Toast.makeText(this, "Đã đạt tối đa 10 ảnh!", Toast.LENGTH_SHORT).show();
            } else {
                pickMultipleImagesLauncher.launch("image/*");
            }
        });
    }

    private void setupDropdownMenu() {
        String[] categories = com.example.marketplace.utils.CategoryHelper.getCategoryNames();
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, categories);
        binding.actCategory.setAdapter(adapter);
    }

    private void setupAddressDropdowns() {
        // Đã sửa requireContext() thành this
        com.example.marketplace.utils.AddressParser.loadHanoiData(this);

        List<String> districts = AddressParser.getDistricts();
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, districts);
        binding.actDistrict.setAdapter(districtAdapter);

        binding.actDistrict.setOnItemClickListener((parent, view, position, id) -> {
            String selectedDistrict = (String) parent.getItemAtPosition(position);
            binding.actWard.setText("", false);

            List<String> wards = AddressParser.getWards(selectedDistrict);
            ArrayAdapter<String> wardAdapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, wards);
            binding.actWard.setAdapter(wardAdapter);
        });
    }

    private void updateImageCount() {
        binding.tvImageCount.setText(mixedImageUris.size() + "/10 ảnh");
    }

    private void setupListeners() {
        // Nút Back trên màn hình
        binding.btnBack.setOnClickListener(v -> showExitDialog());
        // Nút Cập nhật
        binding.btnUpdate.setOnClickListener(v -> attemptUpdateProduct());
    }

    // HIỂN THỊ DIALOG CẢNH BÁO
    private void showExitDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Hủy thay đổi?")
                .setMessage("Bạn có chắc chắn muốn thoát? Các thay đổi của bạn sẽ không được lưu.")
                .setPositiveButton("Thoát", (dialog, which) -> finish())
                .setNegativeButton("Ở lại", null)
                .show();
    }

    private void attemptUpdateProduct() {
        if (mixedImageUris.isEmpty()) {
            Toast.makeText(this, "Vui lòng giữ lại ít nhất 1 ảnh!", Toast.LENGTH_SHORT).show();
            return;
        }

        List<String> finalUrls = new ArrayList<>();
        List<Uri> newUrisToUpload = new ArrayList<>();

        for (Uri uri : mixedImageUris) {
            String uriStr = uri.toString();
            if (uriStr.startsWith("http")) {
                finalUrls.add(uriStr);
            } else {
                newUrisToUpload.add(uri);
            }
        }

        String title = binding.edtTitle.getText().toString().trim();
        String priceStr = binding.edtPrice.getText().toString().trim();
        String category = binding.actCategory.getText().toString().trim();
        String desc = binding.edtDescription.getText().toString().trim();
        String contactPhone = binding.edtContactPhone.getText().toString().trim();
        String district = binding.actDistrict.getText().toString().trim();
        String ward = binding.actWard.getText().toString().trim();
        String street = binding.edtStreet.getText().toString().trim();

        // MA THUẬT GỘP ĐỊA CHỈ
        String fullAddress = street + ", " + ward + ", " + district + ", Thành phố Hà Nội";

        // Kiểm tra cơ bản
        if (title.isEmpty() || priceStr.isEmpty() || category.isEmpty() || desc.isEmpty() ||
                contactPhone.isEmpty() || district.isEmpty() || ward.isEmpty() || street.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đầy đủ thông tin!", Toast.LENGTH_SHORT).show();
            return;
        }

        double price = Double.parseDouble(priceStr);

        showLoading(true);

        if (newUrisToUpload.isEmpty()) {
            saveToDatabase(title, price, category, desc, contactPhone, fullAddress, finalUrls);
        } else {
            // Đã sửa this thành EditPostActivity.this cho an toàn ngữ cảnh
            viewModel.uploadMultipleImagesToCloud(EditPostActivity.this, newUrisToUpload).observe(this, resource -> {
                if (resource.status == Resource.Status.SUCCESS) {
                    finalUrls.addAll(resource.data);
                    saveToDatabase(title, price, category, desc, contactPhone, fullAddress, finalUrls);
                } else if (resource.status == Resource.Status.ERROR) {
                    showLoading(false);
                    Toast.makeText(this, resource.message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void saveToDatabase(String title, double price, String category, String desc, String contactPhone, String address, List<String> imageUrls) {
        Product product = new Product();
        product.setId(currentProduct.id);
        product.setViewCount(currentProduct.viewCount);
        product.setTimestamp(currentProduct.timestamp);
        product.setSellerId(currentProduct.sellerId);

        product.setTitle(title);
        product.setPrice(price);
        product.setCategory(category);
        product.setDescription(desc);
        product.setContactPhone(contactPhone);
        product.setAddress(address);
        product.setImageUrls(imageUrls);

        viewModel.createProduct(product).observe(this, resource -> {
            if (resource.status == Resource.Status.SUCCESS) {
                showLoading(false);
                Toast.makeText(this, "Đã cập nhật bài đăng!", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void showLoading(boolean isLoading) {
        binding.progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        binding.btnUpdate.setText(isLoading ? "" : "Cập Nhật Bài Đăng");
        binding.btnUpdate.setEnabled(!isLoading);
        binding.btnBack.setEnabled(!isLoading);
        binding.btnAddImages.setEnabled(!isLoading);
    }
}