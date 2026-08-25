package com.threadtrades.swap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
class SwapControllerTest {

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

    /** Mutually likes offeredItemId/targetItemId between the two tokens and returns the resulting matchId. */
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
                .andExpect(jsonPath("$.matched").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number matchId = JsonPath.read(responseJson, "$.matchId");
        return matchId.longValue();
    }

    @Test
    void newMatchStartsWithAPendingSwapNeitherSideHasAccepted() throws Exception {
        Cookie tokenA = registerAndGetToken("alma@example.com", "alma");
        Cookie tokenB = registerAndGetToken("boyd@example.com", "boyd");
        Long itemA = uploadItem(tokenA, "Alma's Jacket");
        Long itemB = uploadItem(tokenB, "Boyd's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(get("/api/matches/" + matchId + "/swap").cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.iAccepted").value(false))
                .andExpect(jsonPath("$.otherAccepted").value(false));
    }

    @Test
    void swapOnlyFinalizesOnceBothUsersAcceptAndThenBothItemsAreUnavailable() throws Exception {
        Cookie tokenA = registerAndGetToken("cara@example.com", "cara");
        Cookie tokenB = registerAndGetToken("dale@example.com", "dale");
        Long itemA = uploadItem(tokenA, "Cara's Jacket");
        Long itemB = uploadItem(tokenB, "Dale's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.iAccepted").value(true))
                .andExpect(jsonPath("$.otherAccepted").value(false));

        // From B's side, "iAccepted" is B's own flag (still false) and "otherAccepted" reflects A.
        mockMvc.perform(get("/api/matches/" + matchId + "/swap").cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.iAccepted").value(false))
                .andExpect(jsonPath("$.otherAccepted").value(true));

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"))
                .andExpect(jsonPath("$.iAccepted").value(true))
                .andExpect(jsonPath("$.otherAccepted").value(true));

        mockMvc.perform(get("/api/clothing-items/" + itemA).cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SWAPPED"));
        mockMvc.perform(get("/api/clothing-items/" + itemB).cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SWAPPED"));

        // Swapped items no longer show up in anyone's swipe deck.
        Long itemC = uploadItem(tokenA, "Cara's Boots");
        mockMvc.perform(get("/api/swipes/deck")
                        .param("offeredItemId", itemC.toString())
                        .cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + itemB + ")]").isEmpty());
    }

    @Test
    void rejectingAPendingSwapMarksItRejectedAndBlocksFurtherDecisions() throws Exception {
        Cookie tokenA = registerAndGetToken("eve@example.com", "eve");
        Cookie tokenB = registerAndGetToken("finn@example.com", "finn");
        Long itemA = uploadItem(tokenA, "Eve's Jacket");
        Long itemB = uploadItem(tokenB, "Finn's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/reject").with(csrf()).cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenB))
                .andExpect(status().isConflict());
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/reject").with(csrf()).cookie(tokenA))
                .andExpect(status().isConflict());

        mockMvc.perform(get("/api/clothing-items/" + itemA).cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AVAILABLE"));
    }

    @Test
    void acceptingAnAlreadyAcceptedSwapIsAConflict() throws Exception {
        Cookie tokenA = registerAndGetToken("gwen@example.com", "gwen");
        Cookie tokenB = registerAndGetToken("hank@example.com", "hank");
        Long itemA = uploadItem(tokenA, "Gwen's Jacket");
        Long itemB = uploadItem(tokenB, "Hank's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenB))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    void userNotPartOfTheMatchCannotSeeOrDecideItsSwap() throws Exception {
        Cookie tokenA = registerAndGetToken("ivy@example.com", "ivy");
        Cookie tokenB = registerAndGetToken("jack@example.com", "jack");
        Cookie outsiderToken = registerAndGetToken("kim@example.com", "kim");
        Long itemA = uploadItem(tokenA, "Ivy's Jacket");
        Long itemB = uploadItem(tokenB, "Jack's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(get("/api/matches/" + matchId + "/swap").cookie(outsiderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(
                        patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(outsiderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void swapEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/matches/1/swap")).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/matches/1/swap/accept").with(csrf())).andExpect(status().isUnauthorized());
        mockMvc.perform(patch("/api/matches/1/swap/reject").with(csrf())).andExpect(status().isUnauthorized());
    }
}
