package com.threadtrades.security;

import com.threadtrades.user.AppUser;
import com.threadtrades.user.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final AppUserAuthCache authCache;

    public JwtAuthenticationFilter(
            JwtService jwtService, AppUserRepository appUserRepository, AppUserAuthCache authCache) {
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.authCache = authCache;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var cookie = WebUtils.getCookie(request, SessionCookieService.COOKIE_NAME);
        if (cookie != null) {
            String token = cookie.getValue();
            jwtService.parseToken(token).ifPresent(this::authenticate);
        }
        filterChain.doFilter(request, response);
    }

    // A banned account's already-issued JWTs are rejected here too, not just at
    // login -- otherwise a ban wouldn't take effect until the token's natural
    // expiry (app.jwt.expiration-ms, 24h by default). The granted authority is
    // likewise built from the current DB role, not the JWT claim (user.role()),
    // so an admin promotion/demotion takes effect on the very next request
    // instead of waiting for the token to expire or the user to log in again.
    // AppUserAuthCache short-circuits the DB lookup this requires on most
    // requests; AdminUserService invalidates it on ban/unban/promote/demote so
    // this stays exactly as immediate as a plain per-request DB hit.
    private void authenticate(AuthenticatedUser user) {
        AppUserAuthCache.Entry entry =
                authCache.get(user.appUserId()).orElseGet(() -> loadAndCache(user.appUserId()));
        if (entry == null || entry.banned()) {
            return;
        }
        var authority = new SimpleGrantedAuthority("ROLE_" + entry.role().name());
        var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private AppUserAuthCache.Entry loadAndCache(Long appUserId) {
        AppUser appUser = appUserRepository.findById(appUserId).orElse(null);
        if (appUser == null) {
            return null;
        }
        return authCache.put(appUserId, appUser.isBanned(), appUser.getRole());
    }
}
