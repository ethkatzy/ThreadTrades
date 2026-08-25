package com.threadtrades.ws;

import com.threadtrades.security.JwtService;
import com.threadtrades.security.SessionCookieService;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * Reads the session cookie off the raw WS handshake request (a normal HTTP
 * upgrade request, so browsers attach cookies to it same as any other
 * same-origin/allowed-origin request) and, if it's a valid JWT, stashes the
 * authenticated user as a WebSocket session attribute -- STOMP CONNECT
 * frames themselves carry no such cookie (they're not HTTP requests), so
 * StompAuthChannelInterceptor reads it back out of the session attributes
 * this populates rather than parsing a STOMP header. Always lets the
 * handshake through; a missing/invalid cookie just leaves the attribute
 * unset, and StompAuthChannelInterceptor rejects the CONNECT frame itself
 * exactly as it did for a missing/invalid Authorization header before.
 */
@Component
public class SessionCookieHandshakeInterceptor implements HandshakeInterceptor {

    private final JwtService jwtService;

    public SessionCookieHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            Cookie[] cookies = servletRequest.getServletRequest().getCookies();
            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (cookie.getName().equals(SessionCookieService.COOKIE_NAME)) {
                        jwtService
                                .parseToken(cookie.getValue())
                                .ifPresent(user -> attributes.put(WsSessionAttributes.AUTHENTICATED_USER, user));
                        break;
                    }
                }
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler, Exception exception) {
        // No-op.
    }
}
