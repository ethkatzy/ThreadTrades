package com.threadtrades.security;

import com.threadtrades.user.AppUser;
import com.threadtrades.user.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;

    public JwtAuthenticationFilter(JwtService jwtService, AppUserRepository appUserRepository) {
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            String token = header.substring(BEARER_PREFIX.length());
            jwtService.parseToken(token).ifPresent(user -> {
                // A banned account's already-issued JWTs are rejected here too, not just
                // at login -- otherwise a ban wouldn't take effect until the token's
                // natural expiry (app.jwt.expiration-ms, 24h by default). This adds one
                // indexed PK lookup to every authenticated request; the alternative
                // (banned users keep working for up to a day) isn't an acceptable
                // trade-off for a moderation action meant to be immediate.
                AppUser appUser = appUserRepository.findById(user.appUserId()).orElse(null);
                if (appUser == null || appUser.isBanned()) {
                    return;
                }
                // The granted authority is built from this freshly-fetched row's role, not
                // the JWT claim (user.role()) -- reuses the same lookup already done for the
                // ban check, so an admin promotion/demotion takes effect on the very next
                // request instead of waiting for the token to expire or the user to log in
                // again, matching the immediacy the ban check already guarantees.
                var authority = new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name());
                var authentication = new UsernamePasswordAuthenticationToken(user, null, List.of(authority));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        filterChain.doFilter(request, response);
    }
}
