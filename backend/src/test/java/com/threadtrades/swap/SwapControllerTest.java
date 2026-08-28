package com.threadtrades.swap;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

import com.jayway.jsonpath.JsonPath;
import com.threadtrades.TestcontainersConfiguration;
import com.threadtrades.security.SessionCookieService;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
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

    @Test
    void addingExtraItemResetsAcceptanceAndValidatesAvailabilityBaseItemAndDuplicates() throws Exception {
        Cookie tokenA = registerAndGetToken("liam@example.com", "liam");
        Cookie tokenB = registerAndGetToken("mona@example.com", "mona");
        Cookie tokenC = registerAndGetToken("nora@example.com", "nora");
        Cookie tokenD = registerAndGetToken("owen@example.com", "owen");
        Long itemA1 = uploadItem(tokenA, "Liam's Jacket");
        Long itemB1 = uploadItem(tokenB, "Mona's Jacket");
        Long matchAB = createMatch(tokenA, itemA1, tokenB, itemB1);

        // Complete this unrelated swap so itemA1 becomes SWAPPED -- used below as a
        // "no longer available" item to add elsewhere.
        mockMvc.perform(patch("/api/matches/" + matchAB + "/swap/accept").with(csrf()).cookie(tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/matches/" + matchAB + "/swap/accept").with(csrf()).cookie(tokenB))
                .andExpect(status().isOk());

        Long itemA2 = uploadItem(tokenA, "Liam's Boots");
        Long itemC1 = uploadItem(tokenC, "Nora's Jacket");
        Long itemD1 = uploadItem(tokenD, "Owen's Jacket");
        Long matchAC = createMatch(tokenA, itemA2, tokenC, itemC1);

        // Owned by someone who isn't part of this match at all -- not ownership by the caller,
        // ownership by *either match participant* is what's required.
        mockMvc.perform(post("/api/matches/" + matchAC + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + itemD1 + "}")
                        .cookie(tokenA))
                .andExpect(status().isBadRequest());

        // Already SWAPPED, from the unrelated match above.
        mockMvc.perform(post("/api/matches/" + matchAC + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + itemA1 + "}")
                        .cookie(tokenA))
                .andExpect(status().isBadRequest());

        // Already the match's own base item.
        mockMvc.perform(post("/api/matches/" + matchAC + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + itemA2 + "}")
                        .cookie(tokenA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(patch("/api/matches/" + matchAC + "/swap/accept").with(csrf()).cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iAccepted").value(true));

        Long itemA3 = uploadItem(tokenA, "Liam's Scarf");
        mockMvc.perform(post("/api/matches/" + matchAC + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + itemA3 + "}")
                        .cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myExtraItems[0].item.id").value(itemA3.intValue()))
                .andExpect(jsonPath("$.myExtraItems[0].proposedByMe").value(true))
                .andExpect(jsonPath("$.iAccepted").value(false));

        mockMvc.perform(get("/api/matches/" + matchAC + "/swap").cookie(tokenC))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherExtraItems[0].item.id").value(itemA3.intValue()))
                .andExpect(jsonPath("$.otherExtraItems[0].proposedByMe").value(false));

        mockMvc.perform(post("/api/matches/" + matchAC + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + itemA3 + "}")
                        .cookie(tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    void requestingTheOtherSidesItemMarksItAsARequestOnItsOwningSide() throws Exception {
        Cookie tokenA = registerAndGetToken("penny@example.com", "penny");
        Cookie tokenB = registerAndGetToken("quill@example.com", "quill");
        Long itemA = uploadItem(tokenA, "Penny's Jacket");
        Long itemB = uploadItem(tokenB, "Quill's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);
        Long itemB2 = uploadItem(tokenB, "Quill's Boots");

        mockMvc.perform(get("/api/matches/" + matchId + "/swap/other-items").cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(itemB2.intValue()))
                .andExpect(jsonPath("$.length()").value(1));

        // A requests B's second item -- it's on B's side (B owns it), but A proposed it.
        mockMvc.perform(post("/api/matches/" + matchId + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + itemB2 + "}")
                        .cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otherExtraItems[0].item.id").value(itemB2.intValue()))
                .andExpect(jsonPath("$.otherExtraItems[0].proposedByMe").value(true));

        // From B's side, the requested item shows up as B's own (myExtraItems), but B didn't
        // propose it -- proposedByMe is false, distinguishing "requested from you" from "offered".
        mockMvc.perform(get("/api/matches/" + matchId + "/swap").cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myExtraItems[0].item.id").value(itemB2.intValue()))
                .andExpect(jsonPath("$.myExtraItems[0].proposedByMe").value(false));
    }

    @Test
    void removingAnExtraItemIsAllowedByEitherParticipantAndResetsAcceptance() throws Exception {
        Cookie tokenA = registerAndGetToken("oscar@example.com", "oscar");
        Cookie tokenB = registerAndGetToken("pia@example.com", "pia");
        Long itemA1 = uploadItem(tokenA, "Oscar's Jacket");
        Long itemB1 = uploadItem(tokenB, "Pia's Jacket");
        Long matchId = createMatch(tokenA, itemA1, tokenB, itemB1);
        Long itemA2 = uploadItem(tokenA, "Oscar's Boots");

        mockMvc.perform(post("/api/matches/" + matchId + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + itemA2 + "}")
                        .cookie(tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iAccepted").value(true));

        // B (who didn't add it) can still decline/remove A's offered item -- it's a shared
        // negotiation board, and this is no more destructive than any other term change.
        mockMvc.perform(delete("/api/matches/" + matchId + "/swap/extra-items/" + itemA2)
                        .with(csrf())
                        .cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myExtraItems").isEmpty());

        mockMvc.perform(get("/api/matches/" + matchId + "/swap").cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.iAccepted").value(false));

        mockMvc.perform(delete("/api/matches/" + matchId + "/swap/extra-items/" + itemA2)
                        .with(csrf())
                        .cookie(tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void cashTermIsViewerRelativeAndResetsAcceptance() throws Exception {
        Cookie tokenA = registerAndGetToken("quinn@example.com", "quinn");
        Cookie tokenB = registerAndGetToken("ruby@example.com", "ruby");
        Long itemA = uploadItem(tokenA, "Quinn's Jacket");
        Long itemB = uploadItem(tokenB, "Ruby's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/matches/" + matchId + "/swap/cash")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountPence\":0,\"iOwe\":true}")
                        .cookie(tokenA))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/api/matches/" + matchId + "/swap/cash")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountPence\":1500,\"iOwe\":true}")
                        .cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashAmountPence").value(1500))
                .andExpect(jsonPath("$.iOweCash").value(true))
                .andExpect(jsonPath("$.iAccepted").value(false));

        mockMvc.perform(get("/api/matches/" + matchId + "/swap").cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashAmountPence").value(1500))
                .andExpect(jsonPath("$.iOweCash").value(false));

        mockMvc.perform(delete("/api/matches/" + matchId + "/swap/cash").with(csrf()).cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cashAmountPence").doesNotExist())
                .andExpect(jsonPath("$.iOweCash").doesNotExist());
    }

    @Test
    void finalizingASwapWithExtraItemsAndCashMarksAllExtraItemsSwapped() throws Exception {
        Cookie tokenA = registerAndGetToken("sam@example.com", "sam");
        Cookie tokenB = registerAndGetToken("tara@example.com", "tara");
        Long itemA = uploadItem(tokenA, "Sam's Jacket");
        Long itemB = uploadItem(tokenB, "Tara's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);
        Long extraA = uploadItem(tokenA, "Sam's Scarf");
        Long extraB = uploadItem(tokenB, "Tara's Belt");

        mockMvc.perform(post("/api/matches/" + matchId + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + extraA + "}")
                        .cookie(tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/matches/" + matchId + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + extraB + "}")
                        .cookie(tokenB))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/matches/" + matchId + "/swap/cash")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountPence\":500,\"iOwe\":true}")
                        .cookie(tokenA))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").with(csrf()).cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACCEPTED"));

        mockMvc.perform(get("/api/clothing-items/" + extraA).cookie(tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SWAPPED"));
        mockMvc.perform(get("/api/clothing-items/" + extraB).cookie(tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SWAPPED"));

        List<Map<String, Object>> history = JsonPath.read(
                mockMvc.perform(get("/api/swaps/history").cookie(tokenA))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString(),
                "$[?(@.matchId == " + matchId + ")]");
        assertThat(history).hasSize(1);
        assertThat((Number) history.get(0).get("cashAmountPence")).isEqualTo(500);
        assertThat((Boolean) history.get(0).get("iOweCash")).isTrue();
    }

    @Test
    void extraItemAndCashEndpointsRequireAuthenticationAndParticipation() throws Exception {
        Cookie tokenA = registerAndGetToken("uma@example.com", "uma");
        Cookie tokenB = registerAndGetToken("vic@example.com", "vic");
        Cookie outsiderToken = registerAndGetToken("wes@example.com", "wes");
        Long itemA = uploadItem(tokenA, "Uma's Jacket");
        Long itemB = uploadItem(tokenB, "Vic's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(post("/api/matches/" + matchId + "/swap/extra-items").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/matches/" + matchId + "/swap/extra-items/1").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(put("/api/matches/" + matchId + "/swap/cash").with(csrf()))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/matches/" + matchId + "/swap/cash").with(csrf()))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/matches/" + matchId + "/swap/extra-items")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clothingItemId\":" + itemA + "}")
                        .cookie(outsiderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/matches/" + matchId + "/swap/cash")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"amountPence\":100,\"iOwe\":true}")
                        .cookie(outsiderToken))
                .andExpect(status().isNotFound());
    }
}
