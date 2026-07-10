package com.example.marketplace;

import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

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
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private String currentUserId;
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
            // Gắn Bottom Navigation với NavController
            NavigationUI.setupWithNavController(binding.bottomNavigation, navController);
            binding.bottomNavigation.setItemActiveIndicatorColor(null);
        }

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            listenForGlobalMessages();
        }
    }

    private void listenForGlobalMessages() {
        FirebaseManager.getInstance().getDb().collection("ChatRooms")
                .whereArrayContains("participants", currentUserId)
                .addSnapshotListener(this, (snapshots, e) -> {
                    if (e != null || snapshots == null) return;

                    int totalUnreadCount = 0;

                    // 1. TÍNH TỔNG SỐ TIN CHƯA ĐỌC: Duyệt toàn bộ snapshot để Badge luôn chính xác tuyệt đối
                    for (QueryDocumentSnapshot doc : snapshots) {
                        ChatRoom room = doc.toObject(ChatRoom.class);
                        if (!room.isRead() && room.getLastSenderId() != null && !room.getLastSenderId().equals(currentUserId)) {
                            totalUnreadCount++;
                        }
                    }

                    // 2. KIỂM TRA THAY ĐỔI: Chỉ dùng để bắn Popup thông báo khi có tin nhắn mới thực sự đến
                    for (DocumentChange dc : snapshots.getDocumentChanges()) {
                        ChatRoom room = dc.getDocument().toObject(ChatRoom.class);
                        boolean isUnread = !room.isRead() && room.getLastSenderId() != null && !room.getLastSenderId().equals(currentUserId);

                        if (isUnread && !isFirstLoad) {
                            // Chỉ hiện popup nếu người dùng đang không ở trong chính phòng chat đó
                            if (!room.getId().equals(ChatActivity.activeChatRoomId)) {
                                showModernPopup("Tin nhắn mới: " + room.getLastMessage());
                            }
                        }
                    }

                    // 3. XỬ LÝ LẦN ĐẦU VÀO APP (Vị trí con trỏ của bạn)
                    if (isFirstLoad) {
                        isFirstLoad = false;
                        if (totalUnreadCount > 0) {
                            Log.d("MainActivity", "Hệ thống lắng nghe tin nhắn đã sẵn sàng. Chưa đọc: " + totalUnreadCount);
                            showModernPopup("Bạn có " + totalUnreadCount + " cuộc trò chuyện chưa đọc!");
                        }
                    }

                    // 4. CẬP NHẬT BADGE TRÊN THANH ĐIỀU HƯỚNG
                    updateBottomNavBadge(totalUnreadCount);
                });
    }

    private void updateBottomNavBadge(int count) {
        BadgeDrawable badge = binding.bottomNavigation.getOrCreateBadge(R.id.nav_contact);
        if (count > 0) {
            badge.setVisible(true);
            badge.setNumber(count);
        } else {
            badge.setVisible(false);
            badge.clearNumber();
        }
    }

    // Hiệu ứng Popup thông báo trượt từ trên xuống chuyên nghiệp
    private void showModernPopup(String messageText) {
        View popupView = LayoutInflater.from(this).inflate(R.layout.layout_custom_popup, null);
        TextView tvMsg = popupView.findViewById(R.id.tvPopupMessage);
        tvMsg.setText(messageText);

        ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.TOP;
        popupView.setLayoutParams(params);

        decorView.addView(popupView);

        // Animation trượt xuống
        popupView.setTranslationY(-500f);
        popupView.animate().translationY(0f).setDuration(400).start();

        // Tự động biến mất sau 3.5 giây
        new android.os.Handler().postDelayed(() -> {
            if (popupView.getParent() != null) {
                popupView.animate().translationY(-500f).setDuration(400).withEndAction(() -> {
                    decorView.removeView(popupView);
                }).start();
            }
        }, 3500);
    }
}
