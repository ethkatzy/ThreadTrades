package com.threadtrades.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    // Bootstraps the *first* admin for a test: an in-app promote/demote endpoint
    // now exists (see below), but it requires an already-authenticated admin
    // caller, so there's no in-app way to create that first admin from nothing.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String STORAGE_LOCAL_PATH =
            System.getProperty("java.io.tmpdir") + "/threadtrades-test-uploads-" + UUID.randomUUID();

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.local-path", () -> STORAGE_LOCAL_PATH);
    }

    private record Registered(Cookie sessionCookie, Long appUserId) {
    }

    private Registered register(String email, String username) throws Exception {
        String registerJson =
                """
                {"email":"%s","password":"hunter2pass","username":"%s","name":"Test User"}
                """
                        .formatted(email, username);
        var result = mockMvc.perform(post("/api/auth/register").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn();
        String responseJson = result.getResponse().getContentAsString();
        Number userId = JsonPath.read(responseJson, "$.userId");
        return new Registered(result.getResponse().getCookie(SessionCookieService.COOKIE_NAME), userId.longValue());
    }

    private Cookie promoteToAdmin(String email) throws Exception {
        jdbcTemplate.update("UPDATE app_user SET role = 'ADMIN' WHERE email = ?", email);
        // Re-login so the session cookie actually carries the ADMIN role claim.
        String loginJson = """
                {"email":"%s","password":"hunter2pass"}
                """.formatted(email);
        return mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getCookie(SessionCookieService.COOKIE_NAME);
    }

    @Test
    void adminCanListAndBanAndUnbanAUser() throws Exception {
        register("admin1@example.com", "admin1");
        Cookie adminCookie = promoteToAdmin("admin1@example.com");
        Registered target = register("target1@example.com", "target1");

        mockMvc.perform(get("/api/admin/users?query=target1").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'target1')]").exists());

        String targetProfileIdJson = mockMvc.perform(get("/api/admin/users?query=target1").cookie(adminCookie))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number targetProfileId = JsonPath.read(targetProfileIdJson, "$.content[0].id");

        mockMvc.perform(post("/api/admin/users/" + targetProfileId + "/ban").with(csrf()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(true));

        // The banned user's still-valid session is now rejected, not just future logins.
        mockMvc.perform(get("/api/users/me").cookie(target.sessionCookie()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"target1@example.com","password":"hunter2pass"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users/" + targetProfileId + "/unban").with(csrf()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(false));
        mockMvc.perform(post("/api/auth/login").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"target1@example.com","password":"hunter2pass"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void adminCanListUsersWithNoSearchFilter() throws Exception {
        // Regression test: the unfiltered list previously 500'd (surfaced to the
        // client as an opaque failure) because the repository query passed a raw
        // null :query parameter down to Postgres via a JPQL branch Hibernate
        // couldn't infer a type for -- see UserProfileRepository.searchForAdmin.
        register("admin9@example.com", "admin9");
        Cookie adminCookie = promoteToAdmin("admin9@example.com");

        mockMvc.perform(get("/api/admin/users").cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'admin9')]").exists());
    }

    @Test
    void adminCannotBanTheirOwnAccount() throws Exception {
        register("admin2@example.com", "admin2");
        Cookie adminCookie = promoteToAdmin("admin2@example.com");

        String selfJson = mockMvc.perform(get("/api/admin/users?query=admin2").cookie(adminCookie))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number selfProfileId = JsonPath.read(selfJson, "$.content[0].id");

        mockMvc.perform(post("/api/admin/users/" + selfProfileId + "/ban").with(csrf()).cookie(adminCookie))
                .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanPromoteAndDemoteAUser() throws Exception {
        register("admin3@example.com", "admin3");
        Cookie adminCookie = promoteToAdmin("admin3@example.com");
        Registered target = register("target3@example.com", "target3");

        String targetJson = mockMvc.perform(get("/api/admin/users?query=target3").cookie(adminCookie))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number targetProfileId = JsonPath.read(targetJson, "$.content[0].id");

        mockMvc.perform(post("/api/admin/users/" + targetProfileId + "/promote").with(csrf()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("ADMIN"));

        // The promotion takes effect on the target's already-issued session immediately
        // -- no re-login needed -- because JwtAuthenticationFilter re-derives the
        // granted authority from the DB on every request rather than trusting the
        // token's role claim.
        mockMvc.perform(get("/api/admin/users").cookie(target.sessionCookie()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/admin/users/" + targetProfileId + "/demote").with(csrf()).cookie(adminCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("USER"));

        // Demotion is just as immediate: the same still-valid session is now rejected
        // by the admin-only endpoint without needing to expire or re-login.
        mockMvc.perform(get("/api/admin/users").cookie(target.sessionCookie()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCannotDemoteTheirOwnAccount() throws Exception {
        register("admin4@example.com", "admin4");
        Cookie adminCookie = promoteToAdmin("admin4@example.com");

        String selfJson = mockMvc.perform(get("/api/admin/users?query=admin4").cookie(adminCookie))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number selfProfileId = JsonPath.read(selfJson, "$.content[0].id");

        mockMvc.perform(post("/api/admin/users/" + selfProfileId + "/demote").with(csrf()).cookie(adminCookie))
                .andExpect(status().isBadRequest());
    }
}
