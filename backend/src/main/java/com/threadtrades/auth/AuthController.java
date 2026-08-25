package com.threadtrades.auth;

import com.threadtrades.security.SessionCookieService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final SessionCookieService sessionCookieService;

    public AuthController(AuthService authService, SessionCookieService sessionCookieService) {
        this.authService = authService;
        this.sessionCookieService = sessionCookieService;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        AuthResult result = authService.register(request);
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                sessionCookieService.buildSessionCookie(result.token()).toString());
        return ResponseEntity.status(HttpStatus.CREATED).body(result.response());
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResult result = authService.login(request);
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                sessionCookieService.buildSessionCookie(result.token()).toString());
        return result.response();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        response.addHeader(
                HttpHeaders.SET_COOKIE, sessionCookieService.buildExpiredCookie().toString());
        return ResponseEntity.noContent().build();
    }
}
