package com.threadtrades.message;

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
class MessageControllerTest {

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

    private String messageJson(String content) {
        return """
                {"content":"%s"}
                """
                .formatted(content);
    }

    @Test
    void matchedUsersCanSendAndListMessagesInOrder() throws Exception {
        String tokenA = registerAndGetToken("amy@example.com", "amy");
        String tokenB = registerAndGetToken("ben@example.com", "ben");
        Long itemA = uploadItem(tokenA, "Amy's Jacket");
        Long itemB = uploadItem(tokenB, "Ben's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson("Hi Ben!"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.content").value("Hi Ben!"))
                .andExpect(jsonPath("$.matchId").value(matchId.intValue()));

        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson("Hey Amy!"))
                        .header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/matches/" + matchId + "/messages").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].content").value("Hi Ben!"))
                .andExpect(jsonPath("$[1].content").value("Hey Amy!"));
    }

    @Test
    void twoMatchesBetweenTheSameUsersHaveIndependentThreads() throws Exception {
        String tokenA = registerAndGetToken("cleo@example.com", "cleo");
        String tokenB = registerAndGetToken("drew@example.com", "drew");
        Long itemAOne = uploadItem(tokenA, "Cleo's Jacket");
        Long itemBOne = uploadItem(tokenB, "Drew's Jacket");
        Long itemATwo = uploadItem(tokenA, "Cleo's Boots");
        Long itemBTwo = uploadItem(tokenB, "Drew's Boots");

        Long firstMatchId = createMatch(tokenA, itemAOne, tokenB, itemBOne);
        Long secondMatchId = createMatch(tokenA, itemATwo, tokenB, itemBTwo);

        mockMvc.perform(post("/api/matches/" + firstMatchId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson("About the jackets"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/matches/" + secondMatchId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson("About the boots"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        mockMvc.perform(
                        get("/api/matches/" + firstMatchId + "/messages").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("About the jackets"));
        mockMvc.perform(
                        get("/api/matches/" + secondMatchId + "/messages").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].content").value("About the boots"));
    }

    @Test
    void userNotPartOfTheMatchCannotSendOrListMessages() throws Exception {
        String tokenA = registerAndGetToken("finn@example.com", "finn");
        String tokenB = registerAndGetToken("gwen@example.com", "gwen");
        String outsiderToken = registerAndGetToken("hank@example.com", "hank");
        Long itemA = uploadItem(tokenA, "Finn's Jacket");
        Long itemB = uploadItem(tokenB, "Gwen's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(get("/api/matches/" + matchId + "/messages").header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/matches/" + matchId + "/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson("Can I butt in?"))
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void unmatchedUsersCannotMessageEachOtherEvenWithAGuessedMatchId() throws Exception {
        String tokenA = registerAndGetToken("ivy@example.com", "ivy");
        String tokenB = registerAndGetToken("jay@example.com", "jay");
        Long itemA = uploadItem(tokenA, "Ivy's Jacket");
        Long itemB = uploadItem(tokenB, "Jay's Jacket");
        // Dislike, not like -- no match is created.
        mockMvc.perform(post("/api/swipes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(swipeJson(itemA, itemB, "DISLIKE"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/matches/999999/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson("Hi anyway"))
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    @Test
    void messagesRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/matches/1/messages")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/matches/1/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(messageJson("Hi")))
                .andExpect(status().isUnauthorized());
    }
}
