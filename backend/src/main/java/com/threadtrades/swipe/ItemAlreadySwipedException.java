package com.threadtrades.swipe;

public class ItemAlreadySwipedException extends RuntimeException {

    public ItemAlreadySwipedException(Long clothingItemId) {
        super("Item already swiped: " + clothingItemId);
    }
}
