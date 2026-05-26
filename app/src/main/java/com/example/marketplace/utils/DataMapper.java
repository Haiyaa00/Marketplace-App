package com.example.marketplace.utils;

import com.example.marketplace.data.local.ProductEntity;
import com.example.marketplace.data.local.UserEntity;
import com.example.marketplace.model.Product;
import com.example.marketplace.model.User;

import java.util.ArrayList;
import java.util.List;

public class DataMapper {

    // Chuyển đổi User (Firebase) -> UserEntity (Room)
    public static UserEntity mapToUserEntity(User user) {
        if (user == null) return null;
        return new UserEntity(
                user.getUid(),
                user.getName(),
                user.getEmail(),
                user.getPhone(),
                user.getAvatarUrl()
        );
    }

    // Chuyển đổi Product (Firebase) -> ProductEntity (Room)
    public static ProductEntity mapToProductEntity(Product product) {
        if (product == null) return null;
        return new ProductEntity(
                product.getId(),
                product.getSellerId(),
                product.getTitle(),
                product.getPrice(),
                product.getDescription(),
                product.getCategory(),
                product.getImageUrls(),
                product.getTimestamp(),
                product.getContactPhone(),
                product.getAddress(),
                product.getViewCount()
        );
    }

    // Chuyển đổi List<Product> -> List<ProductEntity>
    public static List<ProductEntity> mapToProductEntityList(List<Product> products) {
        List<ProductEntity> entities = new ArrayList<>();
        if (products != null) {
            for (Product p : products) {
                entities.add(mapToProductEntity(p));
            }
        }
        return entities;
    }
}
