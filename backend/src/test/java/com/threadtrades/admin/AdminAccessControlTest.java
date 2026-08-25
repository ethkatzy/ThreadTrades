package com.threadtrades.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.jayway.jsonpath.JsonPath;
import com.threadtrades.TestcontainersConfiguration;
import com.threadtrades.security.SessionCookieService;
import jakarta.servlet.http.Cookie;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Every /api/admin/** route is gated by the same SecurityConfig prefix rule
 * (hasRole("ADMIN")), so this checks the boundary once across all of them
 * rather than duplicating a 401/403 test in each resource's own test class.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminAccessControlTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String STORAGE_LOCAL_PATH =
            System.getProperty("java.io.tmpdir") + "/threadtrades-test-uploads-" + UUID.randomUUID();

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.local-path", () -> STORAGE_LOCAL_PATH);
    }

    private static final String[] ADMIN_LIST_ROUTES = {
        "/api/admin/users",
        "/api/admin/clothing-items",
        "/api/admin/matches",
        "/api/admin/swaps",
        "/api/admin/messages",
        "/api/admin/reviews"
    };

    private Cookie registerAndGetToken(String email, String username) throws Exception {
        String registerJson =
                """
                {"email":"%s","password":"hunter2pass","username":"%s","name":"Test User"}
                """
                        .formatted(email, username);
        return mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getCookie(SessionCookieService.COOKIE_NAME);
    }

    @Test
    void adminRoutesRejectAnonymousRequests() throws Exception {
        for (String route : ADMIN_LIST_ROUTES) {
            mockMvc.perform(get(route)).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void adminRoutesRejectNonAdminUsers() throws Exception {
        Cookie token = registerAndGetToken("regular@example.com", "regular");
        for (String route : ADMIN_LIST_ROUTES) {
            mockMvc.perform(get(route).cookie(token))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    void nonHealthInfoActuatorEndpointsAreAdminOnly() throws Exception {
        mockMvc.perform(get("/actuator/metrics")).andExpect(status().isUnauthorized());

        Cookie token = registerAndGetToken("nonadmin2@example.com", "nonadmin2");
        mockMvc.perform(get("/actuator/metrics").cookie(token))
                .andExpect(status().isForbidden());
    }

    @Test
    void healthAndInfoStayPublic() throws Exception {
        mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info")).andExpect(status().isOk());
    }
}
