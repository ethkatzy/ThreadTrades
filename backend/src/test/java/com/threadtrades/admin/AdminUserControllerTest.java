package com.threadtrades.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.threadtrades.TestcontainersConfiguration;
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

    // Exercises the real production promotion path (ROADMAP.md: manual SQL, no
    // in-app role-assignment endpoint) rather than any test-only backdoor.
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String STORAGE_LOCAL_PATH =
            System.getProperty("java.io.tmpdir") + "/threadtrades-test-uploads-" + UUID.randomUUID();

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.local-path", () -> STORAGE_LOCAL_PATH);
    }

    private record Registered(String token, Long appUserId) {
    }

    private Registered register(String email, String username) throws Exception {
        String registerJson =
                """
                {"email":"%s","password":"hunter2pass","username":"%s","name":"Test User"}
                """
                        .formatted(email, username);
        String responseJson = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String token = JsonPath.read(responseJson, "$.token");
        Number userId = JsonPath.read(responseJson, "$.userId");
        return new Registered(token, userId.longValue());
    }

    private String promoteToAdmin(String email) throws Exception {
        jdbcTemplate.update("UPDATE app_user SET role = 'ADMIN' WHERE email = ?", email);
        // Re-login so the token actually carries the ADMIN role claim.
        String loginJson = """
                {"email":"%s","password":"hunter2pass"}
                """.formatted(email);
        String responseJson = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return JsonPath.read(responseJson, "$.token");
    }

    @Test
    void adminCanListAndBanAndUnbanAUser() throws Exception {
        register("admin1@example.com", "admin1");
        String adminToken = promoteToAdmin("admin1@example.com");
        Registered target = register("target1@example.com", "target1");

        mockMvc.perform(get("/api/admin/users?query=target1").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'target1')]").exists());

        String targetProfileIdJson = mockMvc.perform(get("/api/admin/users?query=target1")
                        .header("Authorization", "Bearer " + adminToken))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number targetProfileId = JsonPath.read(targetProfileIdJson, "$.content[0].id");

        mockMvc.perform(post("/api/admin/users/" + targetProfileId + "/ban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(true));

        // The banned user's still-valid JWT is now rejected, not just future logins.
        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + target.token()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"target1@example.com","password":"hunter2pass"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/admin/users/" + targetProfileId + "/unban")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.banned").value(false));
        mockMvc.perform(post("/api/auth/login")
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
        String adminToken = promoteToAdmin("admin9@example.com");

        mockMvc.perform(get("/api/admin/users").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.username == 'admin9')]").exists());
    }

    @Test
    void adminCannotBanTheirOwnAccount() throws Exception {
        register("admin2@example.com", "admin2");
        String adminToken = promoteToAdmin("admin2@example.com");

        String selfJson = mockMvc.perform(
                        get("/api/admin/users?query=admin2").header("Authorization", "Bearer " + adminToken))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number selfProfileId = JsonPath.read(selfJson, "$.content[0].id");

        mockMvc.perform(
                        post("/api/admin/users/" + selfProfileId + "/ban").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());
    }
}
