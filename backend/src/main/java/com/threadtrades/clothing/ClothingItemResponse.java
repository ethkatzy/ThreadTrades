package com.threadtrades.clothing;

import java.time.Instant;

public record ClothingItemResponse(
        Long id,
        Long ownerId,
        String name,
        String imageUrl,
        String brand,
        String itemType,
        String description,
        ClothingSize clothingSize,
        String colour,
        Condition condition,
        Gender gender,
        Instant createdAt) {

    static ClothingItemResponse from(ClothingItem item) {
        return new ClothingItemResponse(
                item.getId(),
                item.getOwner().getId(),
                item.getName(),
                item.getImageUrl(),
                item.getBrand(),
                item.getItemType(),
                item.getDescription(),
                item.getClothingSize(),
                item.getColour(),
                item.getCondition(),
                item.getGender(),
                item.getCreatedAt());
    }
}
