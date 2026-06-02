package com.example.marketplace.ui.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.marketplace.data.remote.FirebaseManager;
import com.example.marketplace.databinding.ActivityChatBinding;
import com.example.marketplace.databinding.ItemMessageBinding;
import com.example.marketplace.model.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;

import java.util.ArrayList;
import java.util.List;

public class ChatActivity extends AppCompatActivity {

    private ActivityChatBinding binding;
    private MessageAdapter adapter;
    private final List<Message> messageList = new ArrayList<>();

    private String currentUserId;
    private String partnerId;
    private String chatRoomId;
    private FirebaseManager firebaseManager;
    public static String activeChatRoomId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        firebaseManager = FirebaseManager.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Nhận dữ liệu từ Intent truyền sang
        partnerId = getIntent().getStringExtra("PARTNER_ID");
        String partnerName = getIntent().getStringExtra("PARTNER_NAME");

        if (partnerId == null || currentUserId == null) {
            Toast.makeText(this, "Lỗi dữ liệu chat", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        binding.tvChatPartnerName.setText(partnerName);
        binding.btnBack.setOnClickListener(v -> finish());

        // Khởi tạo ChatRoomID duy nhất
        chatRoomId = firebaseManager.getChatRoomId(currentUserId, partnerId);

        setupRecyclerView();
        listenForMessages();

        binding.btnSend.setOnClickListener(v -> sendMessage());
    }

    private void setupRecyclerView() {
        adapter = new MessageAdapter();
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // Cuộn xuống dưới cùng khi có tin nhắn mới
        binding.rvMessages.setLayoutManager(layoutManager);
        binding.rvMessages.setAdapter(adapter);
    }

    private void sendMessage() {
        String text = binding.edtMessage.getText().toString().trim();
        if (text.isEmpty()) return;

        Message message = new Message(currentUserId, text, System.currentTimeMillis());

        // Xóa ô nhập ngay lập tức tạo cảm giác mượt mà
        binding.edtMessage.setText("");

        firebaseManager.sendMessage(chatRoomId, currentUserId, partnerId ,message)
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi gửi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // LẮNG NGHE TIN NHẮN THEO THỜI GIAN THỰC (REALTIME MAGIC)
    private void listenForMessages() {
        firebaseManager.getMessagesReference(chatRoomId)
                .orderBy("timestamp")
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;
                    for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                        if (dc.getType() == com.google.firebase.firestore.DocumentChange.Type.ADDED) {
                            Message message = dc.getDocument().toObject(Message.class);
                            messageList.add(message);
                            adapter.notifyItemInserted(messageList.size() - 1);
                            binding.rvMessages.smoothScrollToPosition(messageList.size() - 1);

                            // NẾU CÓ TIN NHẮN TỪ NGƯỜI KIA -> ĐÁNH DẤU ĐÃ ĐỌC TRÊN DB NGAY LẬP TỨC
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
        // VỪA MỞ PHÒNG CHAT LÀ ĐÁNH DẤU ĐÃ ĐỌC LUÔN (ĐỂ XÓA BADGE Ở BOTTOM NAV)
        if (chatRoomId != null) {
            firebaseManager.markChatAsRead(chatRoomId);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        activeChatRoomId = ""; // Thoát ra thì clear đi
    }

    // ================= ADAPTER NỘI BỘ =================
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

            // Kiểm tra: Nếu mình là người gửi thì hiện bong bóng xanh, ẩn bong bóng xám
            if (message.getSenderId().equals(currentUserId)) {
                holder.binding.layoutSend.setVisibility(View.VISIBLE);
                holder.binding.tvSendText.setText(message.getText());
                holder.binding.layoutReceive.setVisibility(View.GONE);
            } else {
                // Nếu đối phương gửi thì hiện bong bóng xám, ẩn bong bóng xanh
                holder.binding.layoutReceive.setVisibility(View.VISIBLE);
                holder.binding.tvReceiveText.setText(message.getText());
                holder.binding.layoutSend.setVisibility(View.GONE);
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