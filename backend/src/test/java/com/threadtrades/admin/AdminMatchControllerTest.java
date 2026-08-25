package com.threadtrades.admin;

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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminMatchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    private Cookie promoteToAdmin(String email) throws Exception {
        jdbcTemplate.update("UPDATE app_user SET role = 'ADMIN' WHERE email = ?", email);
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

    private Long createMatch(Cookie tokenA, Long itemA, Cookie tokenB, Long itemB) throws Exception {
        mockMvc.perform(post("/api/swipes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemA, itemB, "LIKE"))
                        .cookie(tokenA))
                .andExpect(status().isCreated());
        String responseJson = mockMvc.perform(post("/api/swipes").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemB, itemA, "LIKE"))
                        .cookie(tokenB))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number matchId = JsonPath.read(responseJson, "$.matchId");
        return matchId.longValue();
    }

    @Test
    void adminCanListAndViewAMatchIncludingUnwaitedSwapState() throws Exception {
        Cookie tokenA = registerAndGetToken("matcha@example.com", "matcha");
        Cookie tokenB = registerAndGetToken("matchb@example.com", "matchb");
        Long itemA = uploadItem(tokenA, "Match A Jacket");
        Long itemB = uploadItem(tokenB, "Match B Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        registerAndGetToken("admin4@example.com", "admin4");
        Cookie adminToken = promoteToAdmin("admin4@example.com");

        mockMvc.perform(get("/api/admin/matches").cookie(adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + matchId + ")]").exists());

        // No swap row exists yet -- it's created lazily the first time the swap
        // endpoint is hit, not eagerly on match creation (see SwapService).
        mockMvc.perform(get("/api/admin/matches/" + matchId).cookie(adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userAUsername").value("matcha"))
                .andExpect(jsonPath("$.userBUsername").value("matchb"))
                .andExpect(jsonPath("$.swapId").doesNotExist());
    }
}
