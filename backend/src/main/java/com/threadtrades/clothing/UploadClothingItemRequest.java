package com.threadtrades.clothing;

record UploadClothingItemRequest(
        String name,
        String brand,
        String itemType,
        String description,
        ClothingSize clothingSize,
        String colour,
        Condition condition,
        Gender gender) {
}
