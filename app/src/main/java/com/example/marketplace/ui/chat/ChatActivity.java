package com.example.marketplace.ui.chat;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.PickVisualMediaRequest;
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
    private final ActivityResultLauncher<PickVisualMediaRequest> pickMultipleImagesLauncher =
            registerForActivityResult(new ActivityResultContracts.PickMultipleVisualMedia(5), uris -> {
                if (uris != null && !uris.isEmpty()) {
                    // Không cần code cắt mảng nữa, vì Android đã lo việc khóa giới hạn 5 ảnh rồi!
                    uploadAndSendMultipleImages(uris);
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
        binding.btnAttachImage.setOnClickListener(v -> {
            pickMultipleImagesLauncher.launch(new PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly.INSTANCE)
                    .build());
        });
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
    private void uploadAndSendMultipleImages(java.util.List<Uri> imageUris) {
        Toast.makeText(this, "Đang gửi " + imageUris.size() + " ảnh...", Toast.LENGTH_SHORT).show();
        binding.btnAttachImage.setEnabled(false); // Khóa nút bấm tạm thời

        // Biến mảng 1 phần tử dùng để đếm số ảnh đã xử lý xong (Tránh lỗi trong Lambda)
        final int[] processedCount = {0};

        for (int i = 0; i < imageUris.size(); i++) {
            Uri imageUri = imageUris.get(i);

            // 1. Nén từng ảnh
            Uri compressedUri = ImageUtils.compressImage(this, imageUri);

            // Lưu lại index để cộng vào timestamp chống trùng lặp ID
            final int indexOffset = i;

            // 2. Đẩy lên Cloudinary chạy song song (Concurrent)
            CloudinaryManager.getInstance().uploadImage(this, compressedUri).addOnCompleteListener(task -> {
                processedCount[0]++; // Tăng biến đếm khi 1 ảnh upload xong (dù thành công hay lỗi)

                if (task.isSuccessful() && task.getResult() != null) {
                    String uploadedImageUrl = task.getResult();

                    // MA THUẬT CHỐNG GHI ĐÈ: Cộng thêm indexOffset vào timestamp
                    // Giúp các bức ảnh dù upload xong cùng 1 miligiây cũng không bị trùng ID trên Firestore
                    long uniqueTimestamp = System.currentTimeMillis() + indexOffset;

                    Message message = new Message(currentUserId, "[Hình ảnh]", uploadedImageUrl, uniqueTimestamp);
                    firebaseManager.sendMessage(chatRoomId, currentUserId, partnerId, message);
                } else {
                    Toast.makeText(this, "Lỗi gửi ảnh thứ " + (indexOffset + 1), Toast.LENGTH_SHORT).show();
                }

                // 3. Khi tất cả ảnh đã xử lý xong thì mở khóa nút chọn ảnh
                if (processedCount[0] == imageUris.size()) {
                    binding.btnAttachImage.setEnabled(true);
                }
            });
        }
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

            // ================= LOGIC NHÃN NGÀY THÁNG THÔNG MINH =================
            boolean showDateHeader = false;

            if (position == 0) {
                // Tin nhắn đầu tiên trên cùng luôn luôn hiển thị Nhãn ngày
                showDateHeader = true;
            } else {
                // So sánh tin nhắn hiện tại với tin nhắn ngay trên nó (position - 1)
                Message previousMessage = messageList.get(position - 1);

                // Nếu 2 tin nhắn KHÁC NGÀY NHAU -> Hiển thị Nhãn để ngăn cách
                if (!com.example.marketplace.utils.DateUtils.isSameDay(previousMessage.getTimestamp(), message.getTimestamp())) {
                    showDateHeader = true;
                }
            }

            // Gán chữ và ẩn/hiện cục Nhãn
            if (showDateHeader) {
                holder.binding.cardDateHeader.setVisibility(View.VISIBLE);
                holder.binding.tvDateHeader.setText(com.example.marketplace.utils.DateUtils.getChatDateLabel(message.getTimestamp()));
            } else {
                holder.binding.cardDateHeader.setVisibility(View.GONE);
            }
            // ===================================================================

            // KIỂM TRA MÌNH GỬI HAY ĐỐI PHƯƠNG GỬI (Phần này giữ nguyên của bạn)
            if (message.getSenderId().equals(currentUserId)) {
                holder.binding.layoutSend.setVisibility(View.VISIBLE);
                holder.binding.layoutReceive.setVisibility(View.GONE);
                holder.binding.tvSendTime.setText(formattedTime);

                if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    holder.binding.ivSendImage.setVisibility(View.VISIBLE);
                    holder.binding.tvSendText.setVisibility(View.GONE);
                    Glide.with(holder.itemView.getContext()).load(message.getImageUrl()).into(holder.binding.ivSendImage);
                } else {
                    holder.binding.ivSendImage.setVisibility(View.GONE);
                    holder.binding.tvSendText.setVisibility(View.VISIBLE);
                    holder.binding.tvSendText.setText(message.getText());
                }
            } else {
                holder.binding.layoutReceive.setVisibility(View.VISIBLE);
                holder.binding.layoutSend.setVisibility(View.GONE);
                holder.binding.tvReceiveTime.setText(formattedTime);

                if (message.getImageUrl() != null && !message.getImageUrl().isEmpty()) {
                    holder.binding.ivReceiveImage.setVisibility(View.VISIBLE);
                    holder.binding.tvReceiveText.setVisibility(View.GONE);
                    Glide.with(holder.itemView.getContext()).load(message.getImageUrl()).into(holder.binding.ivReceiveImage);
                } else {
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