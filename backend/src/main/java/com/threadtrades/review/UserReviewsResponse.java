package com.threadtrades.review;

import java.util.List;

public record UserReviewsResponse(
        Long userId, String username, String name, Double averageRating, long reviewCount, List<ReviewResponse> reviews) {
}
