package com.threadtrades.swipe;

import com.threadtrades.clothing.ClothingItem;
import com.threadtrades.clothing.ClothingItemResponse;
import com.threadtrades.review.RatingSummary;
import com.threadtrades.review.ReviewService;
import com.threadtrades.security.AuthenticatedUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/swipes")
@Validated
public class SwipeController {

    private final SwipeService swipeService;
    private final ReviewService reviewService;

    public SwipeController(SwipeService swipeService, ReviewService reviewService) {
        this.swipeService = swipeService;
        this.reviewService = reviewService;
    }

    @GetMapping("/deck")
    public List<ClothingItemResponse> deck(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam Long offeredItemId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int limit) {
        List<ClothingItem> items = swipeService.deck(currentUser.appUserId(), offeredItemId, limit);
        Map<Long, RatingSummary> ratings =
                reviewService.getRatingSummaries(items.stream().map(item -> item.getOwner().getId()).distinct().toList());
        return items.stream()
                .map(item -> {
                    RatingSummary rating = ratings.getOrDefault(item.getOwner().getId(), RatingSummary.NONE);
                    return ClothingItemResponse.from(item, rating.averageRating(), rating.reviewCount());
                })
                .toList();
    }

    @PostMapping
    public ResponseEntity<SwipeResponse> record(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @Valid @RequestBody RecordSwipeRequest request) {
        SwipeOutcome outcome = swipeService.recordSwipe(
                currentUser.appUserId(), request.offeredItemId(), request.clothingItemId(), request.decision());
        return ResponseEntity.status(HttpStatus.CREATED).body(SwipeResponse.from(outcome));
    }
}
