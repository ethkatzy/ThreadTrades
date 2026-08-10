package com.threadtrades.clothing;

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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class ClothingItemControllerTest {

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
    void uploadThenFetchByIdAndImage() throws Exception {
        String token = registerAndGetToken("dana@example.com", "dana");
        MockMultipartFile image =
                new MockMultipartFile("image", "photo.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8));

        String uploadResponseJson = mockMvc.perform(multipart("/api/clothing-items")
                        .file(image)
                        .param("name", "Denim Jacket")
                        .param("brand", "Levi's")
                        .param("itemType", "Jacket")
                        .param("description", "Barely worn")
                        .param("clothingSize", "M")
                        .param("colour", "Blue")
                        .param("condition", "GOOD")
                        .param("gender", "UNISEX")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Denim Jacket"))
                .andExpect(jsonPath("$.itemType").value("Jacket"))
                .andExpect(jsonPath("$.clothingSize").value("M"))
                .andExpect(jsonPath("$.condition").value("GOOD"))
                .andExpect(jsonPath("$.gender").value("UNISEX"))
                .andExpect(jsonPath("$.imageUrl", org.hamcrest.Matchers.startsWith("/uploads/")))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Integer id = JsonPath.read(uploadResponseJson, "$.id");
        String imageUrl = JsonPath.read(uploadResponseJson, "$.imageUrl");

        mockMvc.perform(get("/api/clothing-items/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Denim Jacket"));

        mockMvc.perform(get("/api/clothing-items/mine").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(id));

        // The old app's uploads were write-only (AUDIT.md #5) -- confirm this one is actually servable.
        mockMvc.perform(get(imageUrl))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                        .bytes("fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void uploadRejectsUnsupportedImageType() throws Exception {
        String token = registerAndGetToken("erin@example.com", "erin");
        MockMultipartFile file =
                new MockMultipartFile("image", "notes.txt", "text/plain", "not an image".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/clothing-items")
                        .file(file)
                        .param("name", "Bad Upload")
                        .param("itemType", "Jacket")
                        .param("clothingSize", "M")
                        .param("condition", "GOOD")
                        .param("gender", "UNISEX")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void uploadRejectsMissingRequiredField() throws Exception {
        String token = registerAndGetToken("frank@example.com", "frank");
        MockMultipartFile image =
                new MockMultipartFile("image", "photo.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/clothing-items")
                        .file(image)
                        .param("name", "Missing Fields")
                        .param("clothingSize", "M")
                        .param("condition", "GOOD")
                        .param("gender", "UNISEX")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void uploadRequiresAuthentication() throws Exception {
        MockMultipartFile image =
                new MockMultipartFile("image", "photo.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/api/clothing-items")
                        .file(image)
                        .param("name", "No Auth")
                        .param("itemType", "Jacket")
                        .param("clothingSize", "M")
                        .param("condition", "GOOD")
                        .param("gender", "UNISEX"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getByIdReturnsNotFoundForUnknownItem() throws Exception {
        String token = registerAndGetToken("gina@example.com", "gina");
        mockMvc.perform(get("/api/clothing-items/999999").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
