package com.example.marketplace.ui.detail;

import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;

import java.util.List;

public class ImageSliderAdapter extends RecyclerView.Adapter<ImageSliderAdapter.SliderViewHolder> {

    private final List<String> imageUrls;

    public ImageSliderAdapter(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public SliderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Có thể tái sử dụng file item_product.xml hoặc tạo riêng 1 view chứa ImageView.
        // Ở đây ta code động 1 ImageView cho nhanh và tối ưu
        ImageView imageView = new ImageView(parent.getContext());
        imageView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new SliderViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull SliderViewHolder holder, int position) {
        Glide.with(holder.imageView.getContext())
                .load(imageUrls.get(position))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() { return imageUrls.size(); }

    static class SliderViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        public SliderViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}