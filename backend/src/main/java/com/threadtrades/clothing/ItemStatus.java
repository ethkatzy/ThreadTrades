package com.threadtrades.clothing;

public enum ItemStatus {
    AVAILABLE,
    SWAPPED,
    /** Admin moderation only -- see AdminClothingItemService. Never set by normal app flow. */
    REMOVED
}
