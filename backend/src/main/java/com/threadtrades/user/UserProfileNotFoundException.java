package com.threadtrades.user;

public class UserProfileNotFoundException extends RuntimeException {

    public UserProfileNotFoundException(Long userId) {
        super("User not found: " + userId);
    }
}
