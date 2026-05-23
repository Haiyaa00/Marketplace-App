package com.example.marketplace.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.databinding.ItemProductBinding;

import java.text.DecimalFormat;

public class ProductAdapter extends ListAdapter<ProductEntity, ProductAdapter.ProductViewHolder> {

    // Định nghĩa 2 Interface lắng nghe Click
    public interface OnProductClickListener {
        void onProductClick(ProductEntity product);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(ProductEntity product);
    }

    private OnProductClickListener productClickListener;
    private OnDeleteClickListener deleteClickListener;
    private final boolean showDeleteIcon; // Điều kiện ẩn/hiện thùng rác

    // Khởi tạo Constructor (Có thể truyền true/false để bật tắt thùng rác)
    public ProductAdapter(boolean showDeleteIcon) {
        super(DIFF_CALLBACK);
        this.showDeleteIcon = showDeleteIcon;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.productClickListener = listener;
    }

    public void setOnDeleteClickListener(OnDeleteClickListener listener) {
        this.deleteClickListener = listener;
    }

    private static final DiffUtil.ItemCallback<ProductEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProductEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.id.equals(newItem.id);
        }

        @Override
        public boolean areContentsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.title.equals(newItem.title) && oldItem.price == newItem.price && oldItem.imageUrl.equals(newItem.imageUrl);
        }
    };

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemProductBinding binding = ItemProductBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ProductViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        ProductEntity item = getItem(position);
        holder.bind(item, showDeleteIcon, productClickListener, deleteClickListener);
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ItemProductBinding binding;

        public ProductViewHolder(@NonNull ItemProductBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductEntity product, boolean showDelete,
                         OnProductClickListener productListener, OnDeleteClickListener deleteListener) {

            binding.tvProductTitle.setText(product.title);
            binding.tvProductCategory.setText(product.category);

            DecimalFormat formatter = new DecimalFormat("#,###");
            binding.tvProductPrice.setText(formatter.format(product.price) + " đ");

            Glide.with(itemView.getContext())
                    .load(product.imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(binding.ivProductImage);

            // 1. Điều khiển ẩn hiện nút Thùng rác
            if (showDelete) {
                binding.btnDeleteProduct.setVisibility(View.VISIBLE);
            } else {
                binding.btnDeleteProduct.setVisibility(View.GONE);
            }

            // 2. Click vào toàn bộ Card để xem chi tiết
            binding.layoutProductContent.setOnClickListener(v -> {
                if (productListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    productListener.onProductClick(product);
                }
            });

            // 3. Chỉ click vào icon Thùng rác mới gọi xóa
            binding.btnDeleteProduct.setOnClickListener(v -> {
                if (deleteListener != null && getAdapterPosition() != RecyclerView.NO_POSITION) {
                    deleteListener.onDeleteClick(product);
                }
            });
        }
    }
}