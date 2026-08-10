package com.threadtrades.auth;

public record AuthResponse(String token, Long userId, String username, String name) {
}
