package com.threadtrades.clothing;

public class ClothingItemNotFoundException extends RuntimeException {

    public ClothingItemNotFoundException(Long id) {
        super("Clothing item not found: " + id);
    }
}
