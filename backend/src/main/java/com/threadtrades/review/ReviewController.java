package com.threadtrades.review;

import com.threadtrades.security.AuthenticatedUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Upsert: a first-time review is created (201), submitting again for the
     * same swap updates it in place (200) so a reviewer can revise their rating.
     */
    @PostMapping("/api/matches/{matchId}/review")
    public ResponseEntity<ReviewResponse> submit(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long matchId,
            @Valid @RequestBody(required = false) SubmitReviewRequest request) {
        SubmitReviewRequest body = request == null ? new SubmitReviewRequest(null, null) : request;
        ReviewSubmissionResult result = reviewService.submitReview(currentUser.appUserId(), matchId, body);
        HttpStatus status = result.created() ? HttpStatus.CREATED : HttpStatus.OK;
        return ResponseEntity.status(status).body(result.review());
    }

    @GetMapping("/api/users/{userId}/reviews")
    public UserReviewsResponse forUser(@PathVariable Long userId) {
        return reviewService.listForUser(userId);
    }
}
