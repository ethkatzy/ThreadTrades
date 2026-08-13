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
class SwapControllerTest {

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
    void newMatchStartsWithAPendingSwapNeitherSideHasAccepted() throws Exception {
        String tokenA = registerAndGetToken("alma@example.com", "alma");
        String tokenB = registerAndGetToken("boyd@example.com", "boyd");
        Long itemA = uploadItem(tokenA, "Alma's Jacket");
        Long itemB = uploadItem(tokenB, "Boyd's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(get("/api/matches/" + matchId + "/swap").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.iAccepted").value(false))
                .andExpect(jsonPath("$.otherAccepted").value(false));
    }

    @Test
    void swapOnlyFinalizesOnceBothUsersAcceptAndThenBothItemsAreUnavailable() throws Exception {
        String tokenA = registerAndGetToken("cara@example.com", "cara");
        String tokenB = registerAndGetToken("dale@example.com", "dale");
        Long itemA = uploadItem(tokenA, "Cara's Jacket");
        Long itemB = uploadItem(tokenB, "Dale's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.iAccepted").value(true))
                .andExpect(jsonPath("$.otherAccepted").value(false));

        // From B's side, "iAccepted" is B's own flag (still false) and "otherAccepted" reflects A.
        mockMvc.perform(get("/api/matches/" + matchId + "/swap").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.iAccepted").value(false))
                .andExpect(jsonPath("$.otherAccepted").value(true));

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.iAccepted").value(true))
                .andExpect(jsonPath("$.otherAccepted").value(true));

        mockMvc.perform(get("/api/clothing-items/" + itemA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SWAPPED"));
        mockMvc.perform(get("/api/clothing-items/" + itemB).header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SWAPPED"));

        // Swapped items no longer show up in anyone's swipe deck.
        Long itemC = uploadItem(tokenA, "Cara's Boots");
        mockMvc.perform(get("/api/swipes/deck")
                        .param("offeredItemId", itemC.toString())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + itemB + ")]").isEmpty());
    }

    @Test
    void rejectingAPendingSwapMarksItRejectedAndBlocksFurtherDecisions() throws Exception {
        String tokenA = registerAndGetToken("eve@example.com", "eve");
        String tokenB = registerAndGetToken("finn@example.com", "finn");
        Long itemA = uploadItem(tokenA, "Eve's Jacket");
        Long itemB = uploadItem(tokenB, "Finn's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/reject").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/reject").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/clothing-items/" + itemA).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void acceptingAnAlreadyAcceptedSwapIsAConflict() throws Exception {
        String tokenA = registerAndGetToken("gwen@example.com", "gwen");
        String tokenB = registerAndGetToken("hank@example.com", "hank");
        Long itemA = uploadItem(tokenA, "Gwen's Jacket");
        Long itemB = uploadItem(tokenB, "Hank's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    void userNotPartOfTheMatchCannotSeeOrDecideItsSwap() throws Exception {
        String tokenA = registerAndGetToken("ivy@example.com", "ivy");
        String tokenB = registerAndGetToken("jack@example.com", "jack");
        String outsiderToken = registerAndGetToken("kim@example.com", "kim");
        Long itemA = uploadItem(tokenA, "Ivy's Jacket");
        Long itemB = uploadItem(tokenB, "Jack's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(get("/api/matches/" + matchId + "/swap").header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void swapEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/matches/1/swap")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/matches/1/swap/accept")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/matches/1/swap/reject")).andExpect(status().isUnauthorized());
    }
}
