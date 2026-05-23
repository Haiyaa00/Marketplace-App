package com.example.marketplace.ui.contact;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ContactFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Tạo nhanh TextView hiển thị thông báo tạm thời
        TextView textView = new TextView(requireContext());
        textView.setText("Màn hình Liên hệ / Hỗ trợ\n(Tính năng này đang được phát triển)");
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(18);
        return textView;
    }
}