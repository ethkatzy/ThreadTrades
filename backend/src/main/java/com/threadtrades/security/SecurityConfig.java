package com.threadtrades.security;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins);
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            CsrfCookieFilter csrfCookieFilter,
            JsonAuthenticationEntryPoint authenticationEntryPoint)
            throws Exception {
        http
                // The session JWT now travels as an httpOnly cookie (ROADMAP.md #3) instead of a
                // manually-attached Authorization header, so it rides along automatically on
                // cross-site requests too -- csrf.spa() (Spring Security's built-in SPA preset:
                // a JS-readable XSRF-TOKEN cookie + X-XSRF-TOKEN header double-submit check)
                // closes the CSRF hole that reopens once auth is cookie-based. Login/register are
                // exempt since there's no session yet to hijack.
                .csrf(csrf -> csrf.spa().ignoringRequestMatchers("/api/auth/register", "/api/auth/login"))
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // /ws/** is the STOMP handshake -- authenticated separately via the
                        // session cookie read off the handshake request (see
                        // WebSocketConfig's HandshakeInterceptor) since this raw HTTP upgrade
                        // request isn't a normal REST call the JWT filter should touch.
                        .requestMatchers("/api/auth/**", "/actuator/health", "/actuator/info", "/uploads/**", "/ws/**")
                        .permitAll()
                        // Evaluated after the permitAll() above, so /actuator/health and
                        // /actuator/info stay public (first matching rule wins) -- this only
                        // gates the rest of /actuator/** (metrics, env, ...) plus the admin API.
                        .requestMatchers("/api/admin/**", "/actuator/**")
                        .hasRole("ADMIN")
                        .anyRequest()
                        .authenticated())
                .exceptionHandling(ex -> ex.authenticationEntryPoint(authenticationEntryPoint))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(csrfCookieFilter, BasicAuthenticationFilter.class);
        return http.build();
    }
}
