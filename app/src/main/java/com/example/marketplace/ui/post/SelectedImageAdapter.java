package com.example.marketplace.ui.post;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.marketplace.R;
import java.util.List;

public class SelectedImageAdapter extends RecyclerView.Adapter<SelectedImageAdapter.ImageViewHolder> {

    private final List<Uri> imageUris;
    private final Runnable onImageRemoved;

    public SelectedImageAdapter(List<Uri> imageUris, Runnable onImageRemoved) {
        this.imageUris = imageUris;
        this.onImageRemoved = onImageRemoved;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_selected_image, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        Uri uri = imageUris.get(position);
        Glide.with(holder.itemView.getContext()).load(uri).into(holder.ivImage);

        holder.btnRemove.setOnClickListener(v -> {
            imageUris.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, imageUris.size());
            onImageRemoved.run(); // Cập nhật lại Text "x/10 ảnh"
        });
    }

    @Override
    public int getItemCount() { return imageUris.size(); }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage, btnRemove;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            // Sửa lại R.id tương ứng nếu bạn gõ khác ở item_selected_image.xml
            ivImage = itemView.findViewById(R.id.ivImage);
            btnRemove = itemView.findViewById(R.id.btnRemove);
        }
    }
}