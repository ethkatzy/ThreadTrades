package com.threadtrades.swipe;

public class CannotSwipeOwnItemException extends RuntimeException {

    public CannotSwipeOwnItemException(Long clothingItemId) {
        super("Cannot swipe your own item: " + clothingItemId);
    }
}
