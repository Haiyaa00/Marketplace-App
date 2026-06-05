package com.example.marketplace.ui.chat;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.marketplace.data.remote.CloudinaryManager;
import com.example.marketplace.data.remote.FirebaseManager;
import com.example.marketplace.databinding.ActivityChatBinding;
import com.example.marketplace.databinding.ItemMessageBinding;
import com.example.marketplace.model.Message;
import com.example.marketplace.utils.ImageUtils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private MessageAdapter adapter;
    private final List<Message> messageList = new ArrayList<>();

    public static String activeChatRoomId = "";
    private String currentUserId, partnerId, chatRoomId;
    private FirebaseManager firebaseManager;

    // Định dạng thời gian hiển thị dưới tin nhắn (Ví dụ: 14:05)
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    // Bộ xử lý khi người dùng chọn ảnh
    private final ActivityResultLauncher<String> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    uploadAndSendImage(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseManager = FirebaseManager.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        partnerId = getIntent().getStringExtra("PARTNER_ID");
        String partnerName = getIntent().getStringExtra("PARTNER_NAME");

        if (partnerId == null || currentUserId == null) {
            finish();
            return;
        }

        binding.tvChatPartnerName.setText(partnerName);
        binding.btnBack.setOnClickListener(v -> finish());
        chatRoomId = firebaseManager.getChatRoomId(currentUserId, partnerId);

        setupRecyclerView();
        listenForMessages();

        // Nút gửi Text
        binding.btnSend.setOnClickListener(v -> sendTextMessage());

        // Nút gửi Ảnh
        binding.btnAttachImage.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);
    }

    // ================= GỬI TIN NHẮN VĂN BẢN =================
    private void sendTextMessage() {
        String text = binding.edtMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        // Message chỉ có text, imageUrl để rỗng ("")
        Message message = new Message(currentUserId, text, "", System.currentTimeMillis());
        binding.edtMessage.setText("");

        firebaseManager.sendMessage(chatRoomId, currentUserId, partnerId, message)
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi gửi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ================= GỬI TIN NHẮN HÌNH ẢNH =================
    private void uploadAndSendImage(Uri imageUri) {
        Toast.makeText(this, "Đang gửi ảnh...", Toast.LENGTH_SHORT).show();
        binding.btnAttachImage.setEnabled(false); // Khóa nút ảnh tạm thời

        // Nén ảnh
        Uri compressedUri = ImageUtils.compressImage(this, imageUri);

        // Đẩy lên Cloudinary
        CloudinaryManager.getInstance().uploadImage(this, compressedUri).addOnCompleteListener(task -> {
            binding.btnAttachImage.setEnabled(true);
            if (task.isSuccessful() && task.getResult() != null) {
                String uploadedImageUrl = task.getResult();

                // Gửi Firebase: Có imageUrl, text để thông báo tóm tắt "[Hình ảnh]"
                Message message = new Message(currentUserId, "[Hình ảnh]", uploadedImageUrl, System.currentTimeMillis());
                firebaseManager.sendMessage(chatRoomId, currentUserId, partnerId, message);
            } else {
                Toast.makeText(this, "Lỗi gửi ảnh!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= LẮNG NGHE REALTIME =================
    private void listenForMessages() {
        firebaseManager.getMessagesReference(chatRoomId).orderBy("timestamp").addSnapshotListener((snapshots, e) -> {
            if (e != null || snapshots == null) return;
            for (DocumentChange dc : snapshots.getDocumentChanges()) {
                if (dc.getType() == DocumentChange.Type.ADDED) {
                    Message message = dc.getDocument().toObject(Message.class);
                    messageList.add(message);
                    adapter.notifyItemInserted(messageList.size() - 1);
                    binding.rvMessages.smoothScrollToPosition(messageList.size() - 1);

                    if (!message.getSenderId().equals(currentUserId)) {
                        firebaseManager.markChatAsRead(chatRoomId);
                    }
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        activeChatRoomId = chatRoomId;
        if (chatRoomId != null) firebaseManager.markChatAsRead(chatRoomId);
    }

    @Override
    protected void onPause() {
        super.onPause();
        activeChatRoomId = "";
    }

    // ================= ADAPTER HIỂN THỊ TIN NHẮN VÀ ẢNH =================
    private class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {

        @NonNull
        @Override
        public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ItemMessageBinding itemBinding = ItemMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
            return new MessageViewHolder(itemBinding);
        }

        @Override
        public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
            Message message = messageList.get(position);
            String formattedTime = timeFormat.format(new Date(message.getTimestamp()));

            // KIỂM TRA MÌNH GỬI HAY ĐỐI PHƯƠNG GỬI
            if (message.getSenderId().equals(currentUserId)) {
                holder.binding.layoutSend.setVisibility(View.VISIBLE);
                holder.binding.layoutReceive.setVisibility(View.GONE);

                holder.binding.tvSendTime.setText(formattedTime); // Nhãn thời gian

                // Xử lý nếu là Ảnh
                if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    holder.binding.ivSendImage.setVisibility(View.VISIBLE);
                    holder.binding.tvSendText.setVisibility(View.GONE); // Ẩn chữ "[Hình ảnh]"
                    Glide.with(holder.itemView.getContext()).load(message.getImageUrl()).into(holder.binding.ivSendImage);
                } else {
                    // Nếu là Chữ
                    holder.binding.ivSendImage.setVisibility(View.GONE);
                    holder.binding.tvSendText.setVisibility(View.VISIBLE);
                    holder.binding.tvSendText.setText(message.getText());
                }
            } else {
                holder.binding.layoutReceive.setVisibility(View.VISIBLE);
                holder.binding.layoutSend.setVisibility(View.GONE);

                holder.binding.tvReceiveTime.setText(formattedTime); // Nhãn thời gian

                // Xử lý nếu là Ảnh
                if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    holder.binding.ivReceiveImage.setVisibility(View.VISIBLE);
                    holder.binding.tvReceiveText.setVisibility(View.GONE);
                    Glide.with(holder.itemView.getContext()).load(message.getImageUrl()).into(holder.binding.ivReceiveImage);
                } else {
                    // Nếu là Chữ
                    holder.binding.ivReceiveImage.setVisibility(View.GONE);
                    holder.binding.tvReceiveText.setVisibility(View.VISIBLE);
                    holder.binding.tvReceiveText.setText(message.getText());
                }
            }
        }

        @Override
        public int getItemCount() { return messageList.size(); }

        class MessageViewHolder extends RecyclerView.ViewHolder {
            ItemMessageBinding binding;
            public MessageViewHolder(@NonNull ItemMessageBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }
        }
    }
}