package com.threadtrades.review;

public record RatingSummary(Double averageRating, long reviewCount) {

    public static final RatingSummary NONE = new RatingSummary(null, 0);
}
