package com.threadtrades.review;

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
class ReviewControllerTest {

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

    private Long userId(String token) throws Exception {
        String responseJson = mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number id = JsonPath.read(responseJson, "$.id");
        return id.longValue();
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
                .andExpect(jsonPath("$.matched").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Number matchId = JsonPath.read(responseJson, "$.matchId");
        return matchId.longValue();
    }

    private Long acceptedMatch(String tokenA, String tokenB, String nameA, String nameB) throws Exception {
        Long itemA = uploadItem(tokenA, nameA);
        Long itemB = uploadItem(tokenB, nameB);
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/matches/" + matchId + "/swap/accept").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk());
        return matchId;
    }

    @Test
    void cannotReviewBeforeSwapIsAccepted() throws Exception {
        String tokenA = registerAndGetToken("review-a@example.com", "reviewa");
        String tokenB = registerAndGetToken("review-b@example.com", "reviewb");
        Long itemA = uploadItem(tokenA, "A's Jacket");
        Long itemB = uploadItem(tokenB, "B's Jacket");
        Long matchId = createMatch(tokenA, itemA, tokenB, itemB);

        mockMvc.perform(post("/api/matches/" + matchId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isConflict());
    }

    @Test
    void submittingARatingRecordsItAndMarksSwapReviewedForThatSideOnly() throws Exception {
        String tokenA = registerAndGetToken("review-c@example.com", "reviewc");
        String tokenB = registerAndGetToken("review-d@example.com", "reviewd");
        Long matchId = acceptedMatch(tokenA, tokenB, "C's Jacket", "D's Jacket");

        mockMvc.perform(post("/api/matches/" + matchId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":4,\"comment\":\"Great trade!\"}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Great trade!"))
                .andExpect(jsonPath("$.autoGenerated").value(false));

        mockMvc.perform(get("/api/matches/" + matchId + "/swap").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewed").value(true))
                .andExpect(jsonPath("$.myReviewRating").value(4))
                .andExpect(jsonPath("$.myReviewComment").value("Great trade!"))
                .andExpect(jsonPath("$.myReviewAutoGenerated").value(false));
        mockMvc.perform(get("/api/matches/" + matchId + "/swap").header("Authorization", "Bearer " + tokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewed").value(false))
                .andExpect(jsonPath("$.myReviewRating").doesNotExist());

        Long revieweeId = userId(tokenB);
        mockMvc.perform(get("/api/users/" + revieweeId + "/reviews").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(4.0))
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.reviews[0].rating").value(4))
                .andExpect(jsonPath("$.reviews[0].reviewerUsername").value("reviewc"));
    }

    @Test
    void skippingTheReviewLeavesAnAutoGeneratedFiveStarRating() throws Exception {
        String tokenA = registerAndGetToken("review-e@example.com", "reviewe");
        String tokenB = registerAndGetToken("review-f@example.com", "reviewf");
        Long matchId = acceptedMatch(tokenA, tokenB, "E's Jacket", "F's Jacket");

        mockMvc.perform(post("/api/matches/" + matchId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").doesNotExist())
                .andExpect(jsonPath("$.autoGenerated").value(true));
    }

    @Test
    void submittingAgainUpdatesTheExistingReviewInsteadOfRejectingIt() throws Exception {
        String tokenA = registerAndGetToken("review-g@example.com", "reviewg");
        String tokenB = registerAndGetToken("review-h@example.com", "reviewh");
        Long matchId = acceptedMatch(tokenA, tokenB, "G's Jacket", "H's Jacket");

        mockMvc.perform(post("/api/matches/" + matchId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":3,\"comment\":\"It was fine\"}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.rating").value(3));

        // Revising the rating hits the same endpoint again -- updates in place (200, not 201),
        // and doesn't create a second row (still exactly one review for this swap/reviewer).
        mockMvc.perform(post("/api/matches/" + matchId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5,\"comment\":\"Actually great, changed my mind\"}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Actually great, changed my mind"));

        Long revieweeId = userId(tokenB);
        mockMvc.perform(get("/api/users/" + revieweeId + "/reviews").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewCount").value(1))
                .andExpect(jsonPath("$.averageRating").value(5.0))
                .andExpect(jsonPath("$.reviews[0].rating").value(5));

        mockMvc.perform(get("/api/matches/" + matchId + "/swap").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.myReviewRating").value(5));
    }

    @Test
    void skippingThenLaterLeavingARealRatingReplacesTheAutoGeneratedOne() throws Exception {
        String tokenA = registerAndGetToken("review-o@example.com", "reviewo");
        String tokenB = registerAndGetToken("review-p@example.com", "reviewp");
        Long matchId = acceptedMatch(tokenA, tokenB, "O's Jacket", "P's Jacket");

        mockMvc.perform(post("/api/matches/" + matchId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autoGenerated").value(true));

        mockMvc.perform(post("/api/matches/" + matchId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":2,\"comment\":\"On reflection, not great\"}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(2))
                .andExpect(jsonPath("$.autoGenerated").value(false));
    }

    @Test
    void userNotPartOfTheMatchCannotReview() throws Exception {
        String tokenA = registerAndGetToken("review-i@example.com", "reviewi");
        String tokenB = registerAndGetToken("review-j@example.com", "reviewj");
        String outsiderToken = registerAndGetToken("review-k@example.com", "reviewk");
        Long matchId = acceptedMatch(tokenA, tokenB, "I's Jacket", "J's Jacket");

        mockMvc.perform(post("/api/matches/" + matchId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":5}")
                        .header("Authorization", "Bearer " + outsiderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void reviewsAreVisibleToAnyAuthenticatedUserEvenWithoutAMatch() throws Exception {
        String tokenA = registerAndGetToken("review-l@example.com", "reviewl");
        String tokenB = registerAndGetToken("review-m@example.com", "reviewm");
        String strangerToken = registerAndGetToken("review-n@example.com", "reviewn");
        Long matchId = acceptedMatch(tokenA, tokenB, "L's Jacket", "M's Jacket");

        mockMvc.perform(post("/api/matches/" + matchId + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rating\":2}")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isCreated());

        Long revieweeId = userId(tokenB);
        mockMvc.perform(get("/api/users/" + revieweeId + "/reviews").header("Authorization", "Bearer " + strangerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageRating").value(2.0))
                .andExpect(jsonPath("$.reviewCount").value(1));
    }

    @Test
    void reviewEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(post("/api/matches/1/review")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/users/1/reviews")).andExpect(status().isUnauthorized());
    }
}
