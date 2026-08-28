package com.threadtrades.swap;

public class ExtraItemNoLongerAvailableException extends RuntimeException {

    public ExtraItemNoLongerAvailableException(Long clothingItemId) {
        super("An item added to this swap is no longer available: " + clothingItemId);
    }
}
