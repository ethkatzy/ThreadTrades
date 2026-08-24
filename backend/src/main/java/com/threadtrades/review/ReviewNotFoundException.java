package com.threadtrades.review;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(Long id) {
        super("Review not found: " + id);
    }
}
