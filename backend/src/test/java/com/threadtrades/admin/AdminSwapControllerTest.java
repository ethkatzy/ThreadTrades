package com.threadtrades.admin;

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
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class AdminSwapControllerTest {

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

    private String promoteToAdmin(String email) throws Exception {
        jdbcTemplate.update("UPDATE app_user SET role = 'ADMIN' WHERE email = ?", email);
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

    private Long uploadItem(String token, String name) throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "photo.jpg", "image/jpeg", "fake-jpeg-bytes".getBytes(StandardCharsets.UTF_8));
        String responseJson = mockMvc.perform(multipart("/api/clothing-items")
                        .file(image)
                        .param("name", name)
                        .param("itemType", "Jacket")
                        .param("clothingSize", "M")
                        .param("condition", "GOOD")
                        .param("gender", "UNISEX")
                        .header("Authorization", "Bearer " + token))
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

    private Long createMatch(String tokenA, Long itemA, String tokenB, Long itemB) throws Exception {
        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemA, itemB, "LIKE"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());
        String responseJson = mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemB, itemA, "LIKE"))
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number matchId = JsonPath.read(responseJson, "$.matchId");
        return matchId.longValue();
    }

    /** Lazily creates the match's swap row (see SwapService.findOrCreateSwap) and returns its id. */
    private Long touchSwap(String token, Long matchId) throws Exception {
        String responseJson = mockMvc.perform(
                        get("/api/matches/" + matchId + "/swap").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number swapId = JsonPath.read(responseJson, "$.id");
        return swapId.longValue();
    }

    @Test
    void adminCanForceResolveAStuckSwapToAccepted() throws Exception {
        String tokenA = registerAndGetToken("swapa@example.com", "swapa");
        String tokenB = registerAndGetToken("swapb@example.com", "swapb");
        Long itemA = uploadItem(tokenA, "Swap A Jacket");
        Long itemB = uploadItem(tokenB, "Swap B Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);
        Long swapId = touchSwap(tokenA, matchId);

        registerAndGetToken("admin5@example.com", "admin5");
        String adminToken = promoteToAdmin("admin5@example.com");

        mockMvc.perform(get("/api/admin/swaps?status=PENDING").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[?(@.id == " + swapId + ")]").exists());

        mockMvc.perform(post("/api/admin/swaps/" + swapId + "/force-resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.acceptedByUserA").value(true))
                .andExpect(jsonPath("$.acceptedByUserB").value(true));

        // Matches the real accept flow's side effect: both items become SWAPPED.
        mockMvc.perform(get("/api/clothing-items/" + itemA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SWAPPED"));
    }

    @Test
    void adminCanReverseAnAlreadyDecidedSwap() throws Exception {
        String tokenA = registerAndGetToken("swapc@example.com", "swapc");
        String tokenB = registerAndGetToken("swapd@example.com", "swapd");
        Long itemA = uploadItem(tokenA, "Swap C Jacket");
        Long itemB = uploadItem(tokenB, "Swap D Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);
        Long swapId = touchSwap(tokenA, matchId);

        registerAndGetToken("admin6@example.com", "admin6");
        String adminToken = promoteToAdmin("admin6@example.com");

        mockMvc.perform(post("/api/admin/swaps/" + swapId + "/force-resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"ACCEPTED"}
                                """)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        // Unlike the user-facing endpoint (which 409s on an already-decided swap),
        // admin override is allowed to reverse it.
        mockMvc.perform(post("/api/admin/swaps/" + swapId + "/force-resolve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"REJECTED"}
                                """)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }
}
