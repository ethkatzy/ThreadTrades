package com.threadtrades.swap;

public class ExtraItemNotAvailableException extends RuntimeException {

    public ExtraItemNotAvailableException(Long clothingItemId) {
        super("Item is not available to add to a swap: " + clothingItemId);
    }
}
