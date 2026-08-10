package com.threadtrades.swipe;

public class OfferedItemNotOwnedException extends RuntimeException {

    public OfferedItemNotOwnedException(Long clothingItemId) {
        super("You don't own this item, so it can't be offered: " + clothingItemId);
    }
}
