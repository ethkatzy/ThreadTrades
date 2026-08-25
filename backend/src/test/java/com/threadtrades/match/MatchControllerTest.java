package com.threadtrades.match;

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
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MatchControllerTest {

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

    private Long uploadItem(Cookie token, String name) throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8));
        String responseJson = mockMvc.perform(multipart("/api/clothing-items").with(csrf())
                        .file(image)
                        .param("name", name)
                        .param("itemType", "Jacket")
                        .param("clothingSize", "M")
                        .param("condition", "GOOD")
                        .param("gender", "UNISEX")
                        .cookie(token))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number id = JsonPath.read(responseJson, "$.id");
        return id.longValue();
    }

    private String swipeJson(Long offeredItemId, Long clothingItemId, String decision) {
        return """
                {"offeredItemId":%d,"clothingItemId":%d,"decision":"%s"}
                """
                .formatted(offeredItemId, clothingItemId, decision);
    }

    @Test
    void mutualLikeSurfacesInBothUsersMatchLists() throws Exception {
        Cookie tokenA = registerAndGetToken("mona@example.com", "mona");
        Cookie tokenB = registerAndGetToken("ravi@example.com", "ravi");
        Long itemA = uploadItem(tokenA, "Mona's Jacket");
        Long itemB = uploadItem(tokenB, "Ravi's Jacket");

        mockMvc.perform(post("/api/swipes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemA, itemB, "LIKE"))
                        .cookie(tokenA))
                .andExpect(status().isCreated());
        String secondSwipeResponse = mockMvc.perform(post("/api/swipes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemB, itemA, "LIKE"))
                        .cookie(tokenB))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matched").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number matchId = JsonPath.read(secondSwipeResponse, "$.matchId");

        mockMvc.perform(get("/api/matches").cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].otherUsername").value("ravi"))
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].myItem.id").value(itemA.intValue()))
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].otherItem.id").value(itemB.intValue()));

        mockMvc.perform(get("/api/matches").cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].otherUsername").value("mona"))
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].myItem.id").value(itemB.intValue()))
                .andExpect(jsonPath("$[?(@.id == " + matchId + ")].otherItem.id").value(itemA.intValue()));
    }

    @Test
    void oneSidedLikeProducesNoMatches() throws Exception {
        Cookie tokenA = registerAndGetToken("noor@example.com", "noor");
        Cookie tokenB = registerAndGetToken("otto@example.com", "otto");
        Long itemA = uploadItem(tokenA, "Noor's Jacket");
        Long itemB = uploadItem(tokenB, "Otto's Jacket");

        mockMvc.perform(post("/api/swipes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemA, itemB, "LIKE"))
                        .cookie(tokenA))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/matches").cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.otherUsername == 'noor')]").isEmpty());
    }

    @Test
    void matchesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/matches")).andExpect(status().isUnauthorized());
    }
}
