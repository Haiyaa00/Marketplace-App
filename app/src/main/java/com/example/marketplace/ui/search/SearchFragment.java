package com.example.marketplace.ui.search;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;

import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.databinding.FragmentSearchBinding;
import com.example.marketplace.ui.detail.ProductDetailActivity;
import com.example.marketplace.ui.home.ProductAdapter;
import com.google.android.material.chip.Chip;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SearchFragment extends Fragment {

    private FragmentSearchBinding binding;
    private SearchViewModel viewModel;
    private ProductAdapter productAdapter;

    private String currentQuery = "";
    private String currentCategory = "";
    private int currentSortType = 0; // 0: Mới nhất, 1: Giá tăng, 2: Giá giảm

    private LiveData<List<ProductEntity>> searchLiveData;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSearchBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(this).get(SearchViewModel.class);

        binding.btnBack.setOnClickListener(v -> requireActivity().onBackPressed());

        setupRecyclerView();
        setupSortSpinner();
        setupListeners();
        performSearch();
        setupCategoryChips();
    }

    private void setupRecyclerView() {
        productAdapter = new ProductAdapter(false);
        binding.rvSearchResults.setLayoutManager(new GridLayoutManager(requireContext(), 2));
        binding.rvSearchResults.setAdapter(productAdapter);

        productAdapter.setOnProductClickListener(product -> {
            Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
            intent.putExtra("PRODUCT_ID", product.id);
            startActivity(intent);
        });
    }

    private void setupSortSpinner() {
        String[] sortOptions = {"Mới nhất", "Giá tăng dần", "Giá giảm dần"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item, sortOptions);
        binding.spinnerSort.setAdapter(adapter);

        binding.spinnerSort.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                currentSortType = position;
                performSearch(); // Sắp xếp lại danh sách
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void setupListeners() {
        // 1. Lắng nghe ô gõ Text (Gõ đến đâu tìm đến đó)
        binding.edtRealSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                currentQuery = s.toString().trim();
                performSearch();
            }
        });

        // 2. Lắng nghe các Chip Danh mục (Bấm vào thẻ nào thì lọc danh mục đó)
        binding.chipGroupCategory.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == View.NO_ID) {
                currentCategory = ""; // Bỏ chọn
            } else {
                Chip chip = group.findViewById(checkedId);
                currentCategory = chip.getText().toString();
            }
            performSearch();
        });
    }

    //LỌC & SẮP XẾP
    private void performSearch() {
        if (searchLiveData != null) {
            searchLiveData.removeObservers(getViewLifecycleOwner());
        }

        searchLiveData = viewModel.searchAndFilter(currentQuery, currentCategory);
        searchLiveData.observe(getViewLifecycleOwner(), products -> {
            if (products != null) {
                List<ProductEntity> sortedList = new ArrayList<>(products);

                // Xử lý logic sắp xếp bằng Java cực nhanh (vì dữ liệu ở Local)
                if (currentSortType == 1) { // Giá tăng
                    Collections.sort(sortedList, (p1, p2) -> Double.compare(p1.price, p2.price));
                } else if (currentSortType == 2) { // Giá giảm
                    Collections.sort(sortedList, (p1, p2) -> Double.compare(p2.price, p1.price));
                } // currentSortType == 0 (Mới nhất) thì SQL đã sắp xếp sẵn theo timestamp DESC

                productAdapter.submitList(sortedList);
                binding.tvResultCount.setText("Có " + sortedList.size() + " kết quả");
            }
        });
    }

    private void setupCategoryChips() {
        // Lấy danh sách tên từ CategoryHelper
        String[] categories = com.example.marketplace.utils.CategoryHelper.getCategoryNames();

        for (String categoryName : categories) {
            // "Bơm" file mẫu item_chip_category.xml vào ChipGroup
            com.google.android.material.chip.Chip chip = (com.google.android.material.chip.Chip) getLayoutInflater()
                    .inflate(com.example.marketplace.R.layout.item_chip_category, binding.chipGroupCategory, false);

            chip.setText(categoryName);
            binding.chipGroupCategory.addView(chip);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}