package com.example.marketplace;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.marketplace.data.remote.FirebaseManager;
import com.example.marketplace.databinding.ActivityMainBinding;
import com.example.marketplace.model.ChatRoom;
import com.example.marketplace.ui.chat.ChatActivity;
import com.google.android.material.badge.BadgeDrawable;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentChange;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String currentUserId;
    private boolean isInitialLoad = true;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Thiết lập Navigation Component
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            // Gắn Bottom Navigation với NavController (Ma thuật tự động chuyển trang nằm ở đây)
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
            binding.bottomNavigation.setItemActiveIndicatorColor(null);
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            listenForGlobalMessages();
        }
    }
    private void listenForGlobalMessages() {
        // Bắt buộc truyền chữ 'this' vào addSnapshotListener.
        // Khi MainActivity bị đóng (do đăng xuất), Listener này tự động bị tiêu diệt, không gây lỗi cho tài khoản sau!
        com.example.marketplace.data.remote.FirebaseManager.getInstance().getDb().collection("ChatRooms")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener(this, (snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    int unreadCount = 0;

                    for (com.google.firebase.firestore.DocumentChange dc : snapshots.getDocumentChanges()) {
                        com.example.marketplace.model.ChatRoom room = dc.getDocument().toObject(com.example.marketplace.model.ChatRoom.class);

                        // Logic xác định tin nhắn chưa đọc của người khác gửi cho mình
                        boolean isUnread = !room.isRead() && room.getLastSenderId() != null && !room.getLastSenderId().equals(currentUserId);

                        if (isUnread) {
                            unreadCount++;

                            // Nếu KHÔNG PHẢI lần mở app đầu tiên -> Có người nhắn tới -> Hiện Popup nội dung
                            if (!isFirstLoad && !room.getId().equals(com.example.marketplace.ui.chat.ChatActivity.activeChatRoomId)) {
                                showModernPopup("Tin nhắn mới: " + room.getLastMessage());
                            }
                        }
                    }

                    // Nếu LÀ lần mở app đầu tiên -> Gom lại hiện 1 Popup duy nhất
                    if (isFirstLoad) {
                        isFirstLoad = false;
                        if (unreadCount > 0) {
                            showModernPopup("Bạn có " + unreadCount + " cuộc trò chuyện chưa đọc!");
                        }
                    }

                    updateBottomNavBadge(unreadCount);
                });
    }

    private void updateBottomNavBadge(int count) {
        com.google.android.material.badge.BadgeDrawable badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_contact);
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    // Hiệu ứng Toast trượt từ trên xuống như Zalo/Messenger
    private void showModernPopup(String messageText) {
        // 1. Nạp layout Custom Popup
        android.view.View popupView = android.view.LayoutInflater.from(this).inflate(R.layout.layout_custom_popup, null);
        android.widget.TextView tvMsg = popupView.findViewById(R.id.tvPopupMessage);
        tvMsg.setText(messageText);

        // 2. MA THUẬT KIẾN TRÚC: Gắn vào DecorView (Lớp kính cao nhất của điện thoại) để đè lên mọi Fragment/Thanh điều hướng
        android.view.ViewGroup decorView = (android.view.ViewGroup) getWindow().getDecorView();

        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = android.view.Gravity.TOP;
        popupView.setLayoutParams(params);

        decorView.addView(popupView);

        // 3. Animation trượt xuống cực kỳ mượt mà
        popupView.setTranslationY(-500f); // Bắt đầu ở khuất phía trên
        popupView.animate().translationY(0f).setDuration(400).start(); // Trượt xuống

        // 4. Tự động biến mất sau 3.5 giây
        new android.os.Handler().postDelayed(() -> {
            popupView.animate().translationY(-500f).setDuration(400).withEndAction(() -> {
                decorView.removeView(popupView); // Xóa khỏi màn hình sau khi trượt xong
            }).start();
        }, 3500);
    }
}