package com.threadtrades.swap;

public class ExtraItemAlreadyAddedException extends RuntimeException {

    public ExtraItemAlreadyAddedException(Long clothingItemId) {
        super("Item already added to this swap: " + clothingItemId);
    }
}
