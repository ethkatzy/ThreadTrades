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
        ItemStatus status,
        Instant createdAt,
        Double ownerAverageRating,
        long ownerReviewCount) {

    public static ClothingItemResponse from(ClothingItem item) {
        return from(item, null, 0L);
    }

    public static ClothingItemResponse from(ClothingItem item, Double ownerAverageRating, long ownerReviewCount) {
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
                item.getStatus(),
                item.getCreatedAt(),
                ownerAverageRating,
                ownerReviewCount);
    }
}
