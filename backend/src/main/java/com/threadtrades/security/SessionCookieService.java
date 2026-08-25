package com.threadtrades.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Builds the httpOnly cookie that carries the session JWT. The JWT never
 * appears in a JSON response body or in JS-readable storage -- see
 * ROADMAP.md #3 ("JWT lives in browser localStorage") for why this replaced
 * the original Authorization-header/localStorage scheme.
 */
@Component
public class SessionCookieService {

    public static final String COOKIE_NAME = "tt_session";

    private final boolean secure;
    private final String sameSite;
    private final long maxAgeSeconds;

    public SessionCookieService(
            @Value("${app.cookie.secure}") boolean secure,
            @Value("${app.cookie.same-site}") String sameSite,
            @Value("${app.jwt.expiration-ms}") long jwtExpirationMs) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.maxAgeSeconds = jwtExpirationMs / 1000;
    }

    public ResponseCookie buildSessionCookie(String token) {
        return baseCookie(token).maxAge(maxAgeSeconds).build();
    }

    public ResponseCookie buildExpiredCookie() {
        return baseCookie("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}
