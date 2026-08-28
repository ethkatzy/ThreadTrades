package com.threadtrades.swap;

public class ExtraItemNotOwnedException extends RuntimeException {

    public ExtraItemNotOwnedException(Long clothingItemId) {
        super("Item doesn't belong to either side of this match, so it can't be added to the swap: " + clothingItemId);
    }
}
