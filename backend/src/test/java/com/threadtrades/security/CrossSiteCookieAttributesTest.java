package com.threadtrades.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.threadtrades.TestcontainersConfiguration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Locks in the {@code tt_session}/{@code XSRF-TOKEN} cookie attributes under
 * the cross-site config Render actually runs (COOKIE_SECURE=true,
 * COOKIE_SAME_SITE=None -- see ROADMAP.md #3's Security section). Getting
 * this wrong previously broke auth/CSRF in production while every other test
 * (which runs under the local Lax/insecure defaults) kept passing, since
 * *.onrender.com's frontend/backend split is cross-site there but same-site
 * in every local/CI run -- see the two "Fix XSRF-TOKEN..."/"Proxy /api/**..."
 * commits this test class is meant to guard against regressing.
 *
 * <p>Uses a real embedded server (webEnvironment = RANDOM_PORT), not
 * MockMvc, deliberately: {@code CookieCsrfTokenRepository.saveToken()}
 * writes the XSRF-TOKEN cookie via the servlet container's own
 * {@code HttpServletResponse.addCookie(Cookie)} under Jakarta Servlet 6+
 * (what Tomcat 11 declares), relying on the container to serialize the
 * cookie's SameSite attribute into the actual Set-Cookie header --
 * Spring's MockHttpServletResponse doesn't do that, so a MockMvc-based
 * version of this test fails with SameSite silently missing even though
 * production (real Tomcat) is correct. tt_session doesn't hit this because
 * AuthController writes it via response.addHeader(SET_COOKIE,
 * cookie.toString()) directly (see SessionCookieService), bypassing
 * addCookie() entirely -- that one *would* pass under MockMvc too, but this
 * class tests both the same way for consistency.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class CrossSiteCookieAttributesTest {

    @DynamicPropertySource
    static void crossSiteCookieProperties(DynamicPropertyRegistry registry) {
        registry.add("app.cookie.secure", () -> "true");
        registry.add("app.cookie.same-site", () -> "None");
        // Belt-and-braces alongside TestcontainersConfiguration.jwtSecretRegistrar --
        // app.jwt.secret has no default (by design, see that class), and JwtService
        // is on the eager bean-creation path for a real embedded server.
        registry.add("app.jwt.secret", () -> "test-only-jwt-secret-not-for-production-use");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void sessionCookieCarriesSecureAndSameSiteNoneUnderCrossSiteConfig() {
        String registerJson =
                """
                {"email":"erin@example.com","password":"hunter2pass","username":"erin","name":"Erin Example"}
                """;
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // /api/auth/register is CSRF-exempt (no session to hijack yet), so this
        // needs no CSRF token/cookie dance first.
        ResponseEntity<String> response = restTemplate.postForEntity(
                "http://localhost:" + port + "/api/auth/register", new HttpEntity<>(registerJson, headers), String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        String sessionHeader = firstSetCookieHeaderStartingWith(response, SessionCookieService.COOKIE_NAME + "=");
        assertThat(sessionHeader).contains("Secure").contains("SameSite=None").contains("HttpOnly");
    }

    @Test
    void csrfCookieCarriesSecureAndSameSiteNoneUnderCrossSiteConfig() {
        // Any permitAll endpoint other than /api/auth/register|login triggers
        // CsrfCookieFilter's eager token write -- same as the first real
        // request a browser makes against the deployed app.
        ResponseEntity<String> response =
                restTemplate.getForEntity("http://localhost:" + port + "/actuator/health", String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        String xsrfHeader = firstSetCookieHeaderStartingWith(response, "XSRF-TOKEN=");
        assertThat(xsrfHeader).contains("Secure").contains("SameSite=None");
    }

    private static String firstSetCookieHeaderStartingWith(ResponseEntity<?> response, String prefix) {
        List<String> setCookieHeaders = response.getHeaders().get(HttpHeaders.SET_COOKIE);
        assertThat(setCookieHeaders).as("Set-Cookie headers").isNotNull();
        // Some responses carry more than one Set-Cookie for the same name (e.g. an
        // empty clearing value followed by the real one) -- a browser's cookie jar
        // nets these out to the last value, so mirror that here. See ROADMAP.md #3's
        // TestRestTemplate cookie-jar row.
        return setCookieHeaders.stream()
                .filter(h -> h.startsWith(prefix) && !h.startsWith(prefix + ";"))
                .reduce((first, second) -> second)
                .orElseThrow(() -> new AssertionError("No Set-Cookie header starting with \"" + prefix + "\": " + setCookieHeaders));
    }
}
