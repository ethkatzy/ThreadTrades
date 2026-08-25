package com.threadtrades.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Spring Security's cookie-backed CSRF token is generated lazily -- the
 * XSRF-TOKEN cookie is only actually written once something reads
 * CsrfToken#getToken() (normally a server-rendered form). A pure REST API
 * never does that, so nothing would ever trigger the cookie write and the
 * frontend would never have a token to echo back on mutating requests.
 * Forcing the read here on every request is Spring Security's own
 * documented fix for SPA clients (see the "Integrating with CSRF
 * Protection" reference doc for CookieCsrfTokenRepository).
 */
@Component
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
