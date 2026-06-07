package com.example.marketplace.ui.post;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.databinding.ItemManagePostBinding;
import java.text.DecimalFormat;

public class ManagePostAdapter extends ListAdapter<ProductEntity, ManagePostAdapter.ManageViewHolder> {

    public interface OnPostActionListener {
        void onEdit(ProductEntity product);
        void onDelete(ProductEntity product);
        void onDetailClick(ProductEntity product);
    }

    private final OnPostActionListener listener;

    public ManagePostAdapter(OnPostActionListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }

    private static final DiffUtil.ItemCallback<ProductEntity> DIFF_CALLBACK = new DiffUtil.ItemCallback<ProductEntity>() {
        @Override
        public boolean areItemsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.id.equals(newItem.id);
        }
        @Override
        public boolean areContentsTheSame(@NonNull ProductEntity oldItem, @NonNull ProductEntity newItem) {
            return oldItem.title.equals(newItem.title) && oldItem.viewCount == newItem.viewCount;
        }
    };

    @NonNull
    @Override
    public ManageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemManagePostBinding binding = ItemManagePostBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ManageViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull ManageViewHolder holder, int position) {
        holder.bind(getItem(position), listener);
    }

    static class ManageViewHolder extends RecyclerView.ViewHolder {
        private final ItemManagePostBinding binding;

        public ManageViewHolder(@NonNull ItemManagePostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ProductEntity product, OnPostActionListener listener) {
            binding.tvTitle.setText(product.title);
            binding.tvViews.setText(product.viewCount + " lượt xem"); // Con mắt vẫn giữ nguyên ở đây

            java.text.DecimalFormat formatter = new java.text.DecimalFormat("#,###");
            binding.tvPrice.setText(formatter.format(product.price) + " đ");

            if (product.imageUrls != null && !product.imageUrls.isEmpty()) {
                com.bumptech.glide.Glide.with(itemView.getContext()).load(product.imageUrls.get(0)).into(binding.ivThumb);
            }

            // Gán sự kiện Sửa / Xóa cũ
            binding.btnEdit.setOnClickListener(v -> listener.onEdit(product));
            binding.btnDelete.setOnClickListener(v -> listener.onDelete(product));

            // 2. MỚI THÊM: GÁN SỰ KIỆN CLICK CHO TOÀN BỘ ITEM ĐỂ XEM CHI TIẾT
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDetailClick(product);
                }
            });
        }
    }
}