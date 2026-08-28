package com.threadtrades.swap;

public class ExtraItemAlreadyBaseItemException extends RuntimeException {

    public ExtraItemAlreadyBaseItemException(Long clothingItemId) {
        super("Item is already the matched item for this swap: " + clothingItemId);
    }
}
