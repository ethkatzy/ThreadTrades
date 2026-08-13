package com.threadtrades.user;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.threadtrades.TestcontainersConfiguration;
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

    private String registerAndGetToken(String email, String username) throws Exception {
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
        return JsonPath.read(responseJson, "$.token");
    }

    @Test
    void meReturnsTheAuthenticatedUsersProfile() throws Exception {
        String token = registerAndGetToken("liam@example.com", "liam");

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
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
        String token = registerAndGetToken("mara@example.com", "mara");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
                        .param("name", "Mara Updated")
                        .param("bio", "Loves swapping jackets.")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("mara"))
                .andExpect(jsonPath("$.name").value("Mara Updated"))
                .andExpect(jsonPath("$.bio").value("Loves swapping jackets."));

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Mara Updated"))
                .andExpect(jsonPath("$.bio").value("Loves swapping jackets."));
    }

    @Test
    void blankBioClearsAnExistingBio() throws Exception {
        String token = registerAndGetToken("noah@example.com", "noah");
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
                        .param("name", "Noah")
                        .param("bio", "Temporary bio")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
                        .param("name", "Noah")
                        .param("bio", "")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bio").doesNotExist());
    }

    @Test
    void uploadingAPictureUpdatesProfilePictureUrl() throws Exception {
        String token = registerAndGetToken("olga@example.com", "olga");
        MockMultipartFile image = new MockMultipartFile(
                "image", "avatar.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8));

        String responseJson = mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
                        .file(image)
                        .param("name", "Olga")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String profilePictureUrl = JsonPath.read(responseJson, "$.profilePictureUrl");

        org.junit.jupiter.api.Assertions.assertTrue(profilePictureUrl.startsWith("/uploads/"));
    }

    @Test
    void editingWithABlankNameIsRejected() throws Exception {
        String token = registerAndGetToken("penny@example.com", "penny");

        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me")
                        .param("name", "")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void editingRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart(HttpMethod.PATCH, "/api/users/me").param("name", "Nobody"))
                .andExpect(status().isUnauthorized());
    }
}
