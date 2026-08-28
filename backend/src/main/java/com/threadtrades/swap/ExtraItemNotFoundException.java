package com.threadtrades.swap;

public class ExtraItemNotFoundException extends RuntimeException {

    public ExtraItemNotFoundException(Long clothingItemId) {
        super("Item is not part of this swap: " + clothingItemId);
    }
}
