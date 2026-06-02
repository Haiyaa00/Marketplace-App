package com.example.marketplace.ui.contact;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.marketplace.data.remote.FirebaseManager;
import com.example.marketplace.databinding.ItemChatRoomBinding;
import com.example.marketplace.model.ChatRoom;
import com.example.marketplace.model.User;
import com.example.marketplace.ui.chat.ChatActivity;
import com.example.marketplace.utils.DateUtils;
import java.util.List;

public class ChatRoomAdapter extends RecyclerView.Adapter<ChatRoomAdapter.RoomViewHolder> {

    private final List<ChatRoom> roomList;
    private final String currentUserId;

    public ChatRoomAdapter(List<ChatRoom> roomList, String currentUserId) {
        this.roomList = roomList;
        this.currentUserId = currentUserId;
    }

    @NonNull
    @Override
    public RoomViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemChatRoomBinding binding = ItemChatRoomBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new RoomViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull RoomViewHolder holder, int position) {
        ChatRoom room = roomList.get(position);

        String partnerId = room.getParticipants().get(0).equals(currentUserId)
                ? room.getParticipants().get(1) : room.getParticipants().get(0);

        holder.binding.tvLastMessage.setText(room.getLastMessage());
        holder.binding.tvTime.setText(DateUtils.getRelativeTimeSpan(room.getLastTimestamp()));

        // Nếu người gửi cuối KHÔNG PHẢI MÌNH và isRead == false -> Đánh dấu chưa đọc
        boolean isUnread = !room.isRead() && room.getLastSenderId() != null && !room.getLastSenderId().equals(currentUserId);

        if (isUnread) {
            // Chưa đọc -> Bôi đậm chữ, đổi màu đen, hiện chấm xanh
            holder.binding.tvLastMessage.setTypeface(android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.BOLD));
            holder.binding.tvLastMessage.setTextColor(android.graphics.Color.parseColor("#212121")); // Đen đậm
            holder.binding.unreadDot.setVisibility(android.view.View.VISIBLE);
        } else {
            // Đã đọc -> Chữ mỏng, màu xám, ẩn chấm xanh
            holder.binding.tvLastMessage.setTypeface(android.graphics.Typeface.defaultFromStyle(android.graphics.Typeface.NORMAL));
            holder.binding.tvLastMessage.setTextColor(android.graphics.Color.parseColor("#757575")); // Xám nhạt
            holder.binding.unreadDot.setVisibility(android.view.View.GONE);
        }

        // Kéo thông tin đối phương từ Firestore
        FirebaseManager.getInstance().getUserInfo(partnerId).addOnSuccessListener(doc -> {
            if (doc.exists()) {
                User partner = doc.toObject(User.class);
                if (partner != null) {
                    holder.binding.tvPartnerName.setText(partner.getName());
                    Glide.with(holder.itemView.getContext())
                            .load(partner.getAvatarUrl() != null && !partner.getAvatarUrl().isEmpty() ? partner.getAvatarUrl() : android.R.drawable.sym_def_app_icon)
                            .placeholder(android.R.drawable.sym_def_app_icon)
                            .into(holder.binding.ivPartnerAvatar);

                    // Sự kiện click mở ChatActivity
                    holder.itemView.setOnClickListener(v -> {
                        Intent intent = new Intent(holder.itemView.getContext(), ChatActivity.class);
                        intent.putExtra("PARTNER_ID", partnerId);
                        intent.putExtra("PARTNER_NAME", partner.getName());
                        holder.itemView.getContext().startActivity(intent);
                    });
                }
            }
        });
    }

    @Override
    public int getItemCount() { return roomList.size(); }

    static class RoomViewHolder extends RecyclerView.ViewHolder {
        ItemChatRoomBinding binding;
        public RoomViewHolder(ItemChatRoomBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}