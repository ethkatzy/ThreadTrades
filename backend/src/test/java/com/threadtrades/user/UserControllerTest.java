package com.threadtrades.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.jayway.jsonpath.JsonPath;
import com.threadtrades.TestcontainersConfiguration;
import com.threadtrades.security.SessionCookieService;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private static final String STORAGE_LOCAL_PATH =
            System.getProperty("java.io.tmpdir") + "/threadtrades-test-uploads-" + UUID.randomUUID();

    @DynamicPropertySource
    static void storageProperties(DynamicPropertyRegistry registry) {
        registry.add("app.storage.local-path", () -> STORAGE_LOCAL_PATH);
    }

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
    void meReturnsTheAuthenticatedUsersProfile() throws Exception {
        Cookie token = registerAndGetToken("liam@example.com", "liam");

        mockMvc.perform(get("/api/users/me").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("liam"))
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.profilePictureUrl").doesNotExist());
    }

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void editingNameAndBioPersistsAndLeavesUsernameUnchanged() throws Exception {
        Cookie token = registerAndGetToken("mara@example.com", "mara");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me").with(csrf())
                        .param("name", "Mara Updated")
                        .param("bio", "Loves swapping jackets.")
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mara"))
                .andExpect(jsonPath("$.name").value("Mara Updated"))
                .andExpect(jsonPath("$.bio").value("Loves swapping jackets."));

        mockMvc.perform(get("/api/users/me").cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mara Updated"))
                .andExpect(jsonPath("$.bio").value("Loves swapping jackets."));
    }

    @Test
    void blankBioClearsAnExistingBio() throws Exception {
        Cookie token = registerAndGetToken("noah@example.com", "noah");
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me").with(csrf())
                        .param("name", "Noah")
                        .param("bio", "Temporary bio")
                        .cookie(token))
                .andExpect(status().isOk());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me").with(csrf())
                        .param("name", "Noah")
                        .param("bio", "")
                        .cookie(token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").doesNotExist());
    }

    @Test
    void uploadingAPictureUpdatesProfilePictureUrl() throws Exception {
        Cookie token = registerAndGetToken("olga@example.com", "olga");
        MockMultipartFile image = new MockMultipartFile(
                "image", "avatar.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8));

        String responseJson = mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me").with(csrf())
                        .file(image)
                        .param("name", "Olga")
                        .cookie(token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String profilePictureUrl = JsonPath.read(responseJson, "$.profilePictureUrl");

        org.junit.jupiter.api.Assertions.assertTrue(profilePictureUrl.startsWith("/uploads/"));
    }

    @Test
    void editingWithABlankNameIsRejected() throws Exception {
        Cookie token = registerAndGetToken("penny@example.com", "penny");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me").with(csrf())
                        .param("name", "")
                        .cookie(token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editingRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me").with(csrf()).param("name", "Nobody"))
                .andExpect(status().isUnauthorized());
    }
}
