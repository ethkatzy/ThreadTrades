package com.threadtrades.swap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SwapHistoryControllerTest {

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

    /** Mutually likes offeredItemId/targetItemId between the two tokens and returns the resulting matchId. */
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
                .andExpect(jsonPath("$.matched").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number matchId = JsonPath.read(responseJson, "$.matchId");
        return matchId.longValue();
    }

    @Test
    void completedSwapAppearsInBothParticipantsHistoryWithBothItems() throws Exception {
        String tokenA = registerAndGetToken("vera@example.com", "vera");
        String tokenB = registerAndGetToken("walt@example.com", "walt");
        Long itemA = uploadItem(tokenA, "Vera's Jacket");
        Long itemB = uploadItem(tokenB, "Walt's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/swaps/history").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matchId").value(matchId))
                .andExpect(jsonPath("$[0].otherUsername").value("walt"))
                .andExpect(jsonPath("$[0].myItem.id").value(itemA))
                .andExpect(jsonPath("$[0].otherItem.id").value(itemB));

        mockMvc.perform(get("/api/swaps/history").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].matchId").value(matchId))
                .andExpect(jsonPath("$[0].otherUsername").value("vera"))
                .andExpect(jsonPath("$[0].myItem.id").value(itemB))
                .andExpect(jsonPath("$[0].otherItem.id").value(itemA));
    }

    @Test
    void pendingAndRejectedSwapsDoNotAppearInHistory() throws Exception {
        String tokenA = registerAndGetToken("xena@example.com", "xena");
        String tokenB = registerAndGetToken("yara@example.com", "yara");
        Long itemA = uploadItem(tokenA, "Xena's Jacket");
        Long itemB = uploadItem(tokenB, "Yara's Jacket");
        Long pendingMatchId = createMatch(tokenA, itemA, tokenB, itemB);

        Long itemC = uploadItem(tokenA, "Xena's Boots");
        Long itemD = uploadItem(tokenB, "Yara's Boots");
        Long rejectedMatchId = createMatch(tokenA, itemC, tokenB, itemD);
        mockMvc.perform(patch("/api/matches/" + rejectedMatchId + "/swap/reject")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/swaps/history").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // sanity: the pending match's own swap endpoint still reports PENDING, not filtered by mistake.
        mockMvc.perform(get("/api/matches/" + pendingMatchId + "/swap").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void aUserCannotSeeAnotherUsersSwapHistory() throws Exception {
        String tokenA = registerAndGetToken("zane@example.com", "zane");
        String tokenB = registerAndGetToken("abby@example.com", "abby");
        String outsiderToken = registerAndGetToken("cody@example.com", "cody");
        Long itemA = uploadItem(tokenA, "Zane's Jacket");
        Long itemB = uploadItem(tokenB, "Abby's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/swaps/history").header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void swapHistoryRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/swaps/history")).andExpect(status().isUnauthorized());
    }
}
