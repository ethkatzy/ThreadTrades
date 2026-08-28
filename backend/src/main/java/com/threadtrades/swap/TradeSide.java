package com.threadtrades.swap;

/**
 * Which side of a match ({@code userA}/{@code userB} on {@link com.threadtrades.match.ItemMatch})
 * a negotiated extra item or cash term belongs to. Never exposed to the client directly --
 * API responses always translate this to viewer-relative "me"/"other" fields.
 */
public enum TradeSide {
    A,
    B
}
