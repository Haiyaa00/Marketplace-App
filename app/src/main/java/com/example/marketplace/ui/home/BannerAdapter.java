package com.example.marketplace.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.marketplace.R;
import java.util.List;

public class BannerAdapter extends RecyclerView.Adapter<BannerAdapter.BannerViewHolder> {

    private final List<String> bannerUrls;

    public BannerAdapter(List<String> bannerUrls) {
        this.bannerUrls = bannerUrls;
    }

    @NonNull
    @Override
    public BannerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_banner, parent, false);
        return new BannerViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BannerViewHolder holder, int position) {
        // PHÒNG THỦ: Tránh lỗi chia cho 0 (ArithmeticException) gây sập app nếu mảng rỗng
        if (bannerUrls == null || bannerUrls.isEmpty()) return;

        // Dùng phép chia lấy dư để vị trí dù lên đến hàng ngàn vẫn map đúng mảng [1]
        int realPosition = position % bannerUrls.size();

        Glide.with(holder.imageView.getContext())
                .load(bannerUrls.get(realPosition))
                .placeholder(android.R.drawable.ic_menu_gallery)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        // Trả về số cực lớn để vuốt vô tận
        return (bannerUrls == null || bannerUrls.isEmpty()) ? 0 : Integer.MAX_VALUE;
    }

    static class BannerViewHolder extends RecyclerView.ViewHolder {
        // Dùng từ khóa 'final' để tối ưu hóa bộ nhớ và giữ tính bất biến
        final ImageView imageView;

        public BannerViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivBannerImage);
        }
    }
}