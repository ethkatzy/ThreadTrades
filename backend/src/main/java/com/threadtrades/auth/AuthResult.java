package com.threadtrades.auth;

/** Pairs the raw JWT (set as an httpOnly cookie by the controller) with the response body. */
record AuthResult(String token, AuthResponse response) {
}
