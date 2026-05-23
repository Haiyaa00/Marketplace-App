package com.example.marketplace.ui.post;

import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class PostFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Tạo nhanh một TextView bằng Java để không cần tạo file XML tạm
        TextView textView = new TextView(requireContext());
        textView.setText("Màn hình Đăng tin\n(Tính năng này đang được phát triển)");
        textView.setGravity(Gravity.CENTER);
        textView.setTextSize(18);
        return textView;
    }
}
