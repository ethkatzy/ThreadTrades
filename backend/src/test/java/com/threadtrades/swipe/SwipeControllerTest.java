package com.threadtrades.swipe;

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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class SwipeControllerTest {

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

    @Test
    void deckExcludesOwnItemsAndItemsAlreadySwipedForThatOfferedItem() throws Exception {
        String tokenA = registerAndGetToken("ana@example.com", "ana");
        String tokenB = registerAndGetToken("bo@example.com", "bob");
        Long itemA = uploadItem(tokenA, "Ana's Jacket");
        Long itemB = uploadItem(tokenB, "Bo's Jacket");

        // Other tests in this class share the same Postgres container/schema, so assert
        // membership rather than exact deck contents.
        mockMvc.perform(get("/api/swipes/deck")
                        .param("offeredItemId", itemA.toString())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + itemB + ")]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.id == " + itemA + ")]").isEmpty());

        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemA, itemB, "DISLIKE"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matched").value(false));

        mockMvc.perform(get("/api/swipes/deck")
                        .param("offeredItemId", itemA.toString())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + itemB + ")]").isEmpty());
    }

    @Test
    void likeIsScopedToTheOfferedItemNotAnyItemTheSwiperOwns() throws Exception {
        String tokenA = registerAndGetToken("ivy@example.com", "ivy");
        String tokenB = registerAndGetToken("jay@example.com", "jay");
        Long itemAOne = uploadItem(tokenA, "Ivy's Item One");
        Long itemATwo = uploadItem(tokenA, "Ivy's Item Two");
        Long itemB = uploadItem(tokenB, "Jay's Item");

        // Ivy likes Jay's item when offering item one, but dislikes it when offering item two --
        // these must be independent decisions, not a single yes/no on the target item.
        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemAOne, itemB, "LIKE"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemATwo, itemB, "DISLIKE"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matched").value(false));

        // Jay liking item one back (the one Ivy actually offered for a like) matches --
        // liking item two would not have, since Ivy disliked item two's context.
        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemB, itemAOne, "LIKE"))
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matched").value(true));
    }

    @Test
    void mutualLikeCreatesMatch() throws Exception {
        String tokenA = registerAndGetToken("cara@example.com", "cara");
        String tokenB = registerAndGetToken("dev@example.com", "dev");
        Long itemA = uploadItem(tokenA, "Cara's Jacket");
        Long itemB = uploadItem(tokenB, "Dev's Jacket");

        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemA, itemB, "LIKE"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matched").value(false))
                .andExpect(jsonPath("$.matchId").doesNotExist());

        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemB, itemA, "LIKE"))
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.matched").value(true))
                .andExpect(jsonPath("$.matchId").isNotEmpty());
    }

    @Test
    void cannotSwipeOwnItem() throws Exception {
        String token = registerAndGetToken("eli@example.com", "eli");
        Long offeredItem = uploadItem(token, "Eli's Offered Item");
        Long targetItem = uploadItem(token, "Eli's Other Item");

        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(offeredItem, targetItem, "LIKE"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isBadRequest());
    }

    @Test
    void cannotOfferAnItemYouDoNotOwn() throws Exception {
        String tokenA = registerAndGetToken("kai@example.com", "kai");
        String tokenB = registerAndGetToken("lee@example.com", "lee");
        Long itemB = uploadItem(tokenB, "Lee's Item");

        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemB, itemB, "LIKE"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isBadRequest());
    }

    @Test
    void duplicateSwipeReturnsConflict() throws Exception {
        String tokenA = registerAndGetToken("finn@example.com", "finn");
        String tokenB = registerAndGetToken("gwen@example.com", "gwen");
        Long itemA = uploadItem(tokenA, "Finn's Jacket");
        Long itemB = uploadItem(tokenB, "Gwen's Jacket");

        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemA, itemB, "LIKE"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemA, itemB, "DISLIKE"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    void swipeRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(999999L, 999999L, "LIKE")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void swipeOnUnknownItemReturnsNotFound() throws Exception {
        String token = registerAndGetToken("hana@example.com", "hana");
        Long offeredItem = uploadItem(token, "Hana's Item");

        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(offeredItem, 999999L, "LIKE"))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }
}
