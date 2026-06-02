package com.example.marketplace.ui.contact;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.marketplace.data.remote.FirebaseManager;
import com.example.marketplace.databinding.FragmentContactBinding;
import com.example.marketplace.model.ChatRoom;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class ContactFragment extends Fragment {

    private FragmentContactBinding binding;
    private ChatRoomAdapter adapter;
    private final List<ChatRoom> chatRooms = new ArrayList<>();
    private String currentUserId;

    // Biến quản lý Listener để tránh tràn bộ nhớ khi đóng tab
    private ListenerRegistration chatRoomListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentContactBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            setupRecyclerView();
            listenForChatRooms();
        }
    }

    private void setupRecyclerView() {
        adapter = new ChatRoomAdapter(chatRooms, currentUserId);
        binding.rvChatRooms.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.rvChatRooms.setAdapter(adapter);
    }

    private void listenForChatRooms() {
        // Query Lấy tất cả phòng chat mà mảng "participants" có chứa UID của mình, sắp xếp mới nhất lên đầu
        chatRoomListener = FirebaseManager.getInstance().getDb().collection("ChatRooms")
                .whereArrayContains("participants", currentUserId)
                .orderBy("lastTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    // Làm mới danh sách khi có thay đổi (Để cập nhật tin nhắn cuối cùng lên đầu)
                    chatRooms.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        ChatRoom room = doc.toObject(ChatRoom.class);
                        if (room != null) chatRooms.add(room);
                    }

                    adapter.notifyDataSetChanged();

                    // Xử lý Giao diện rỗng
                    if (chatRooms.isEmpty()) {
                        binding.layoutEmpty.setVisibility(View.VISIBLE);
                        binding.rvChatRooms.setVisibility(View.GONE);
                    } else {
                        binding.layoutEmpty.setVisibility(View.GONE);
                        binding.rvChatRooms.setVisibility(View.VISIBLE);
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Cực kỳ quan trọng: Gỡ bỏ Listener khi chuyển sang Tab khác để tránh lỗi rò rỉ RAM
        if (chatRoomListener != null) {
            chatRoomListener.remove();
        }
        binding = null;
    }
}