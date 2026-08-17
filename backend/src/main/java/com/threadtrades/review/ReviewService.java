package com.threadtrades.review;

import com.threadtrades.match.ItemMatch;
import com.threadtrades.match.ItemMatchRepository;
import com.threadtrades.match.MatchNotFoundException;
import com.threadtrades.swap.Swap;
import com.threadtrades.swap.SwapRepository;
import com.threadtrades.swap.SwapStatus;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileNotFoundException;
import com.threadtrades.user.UserProfileRepository;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewService {

    private static final int AUTO_GENERATED_RATING = 5;

    private final ReviewRepository reviewRepository;
    private final SwapRepository swapRepository;
    private final ItemMatchRepository itemMatchRepository;
    private final UserProfileRepository userProfileRepository;

    public ReviewService(
            ReviewRepository reviewRepository,
            SwapRepository swapRepository,
            ItemMatchRepository itemMatchRepository,
            UserProfileRepository userProfileRepository) {
        this.reviewRepository = reviewRepository;
        this.swapRepository = swapRepository;
        this.itemMatchRepository = itemMatchRepository;
        this.userProfileRepository = userProfileRepository;
    }

    /**
     * Submitting again for the same (swap, reviewer) updates the existing row in
     * place rather than being rejected -- lets a reviewer revise their rating or
     * comment later, same "one row of real state" shape as {@link Swap} accept/reject.
     */
    @Transactional
    public ReviewSubmissionResult submitReview(Long appUserId, Long matchId, SubmitReviewRequest request) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = swapRepository
                .findByMatchId(matchId)
                .filter(s -> s.getStatus() == SwapStatus.ACCEPTED)
                .orElseThrow(() -> new SwapNotCompletedException(matchId));

        boolean viewerIsA = match.getUserA().getId().equals(viewer.getId());
        UserProfile reviewee = viewerIsA ? match.getUserB() : match.getUserA();

        boolean skipped = request.rating() == null;
        int rating = skipped ? AUTO_GENERATED_RATING : request.rating();
        String comment = skipped ? null : request.comment();

        Optional<Review> existing = reviewRepository.findBySwapIdAndReviewerId(swap.getId(), viewer.getId());
        Review review;
        if (existing.isPresent()) {
            review = existing.get();
            review.update(rating, comment, skipped);
        } else {
            review = new Review(swap, viewer, reviewee, rating, comment, skipped);
        }
        reviewRepository.save(review);
        return new ReviewSubmissionResult(ReviewResponse.from(review), existing.isEmpty());
    }

    @Transactional(readOnly = true)
    public UserReviewsResponse listForUser(Long revieweeProfileId) {
        UserProfile reviewee = userProfileRepository
                .findById(revieweeProfileId)
                .orElseThrow(() -> new UserProfileNotFoundException(revieweeProfileId));
        List<ReviewResponse> reviews = reviewRepository.findByRevieweeIdOrderByCreatedAtDesc(revieweeProfileId).stream()
                .map(ReviewResponse::from)
                .toList();
        ReviewRepository.RatingSummary summary = reviewRepository.getRatingSummary(revieweeProfileId);
        return new UserReviewsResponse(
                reviewee.getId(),
                reviewee.getUsername(),
                reviewee.getName(),
                summary.getAverageRating(),
                summary.getReviewCount() == null ? 0 : summary.getReviewCount(),
                reviews);
    }

    /**
     * Batched so screens showing many owners at once (the swipe deck, the matches
     * list) don't fire one aggregate query per item -- callers default missing
     * entries (no reviews yet) to {@link RatingSummary#NONE}.
     */
    @Transactional(readOnly = true)
    public Map<Long, RatingSummary> getRatingSummaries(Collection<Long> revieweeProfileIds) {
        if (revieweeProfileIds.isEmpty()) {
            return Map.of();
        }
        return reviewRepository.getRatingSummaries(revieweeProfileIds).stream()
                .collect(Collectors.toMap(
                        ReviewRepository.UserRatingSummary::getUserId,
                        summary -> new RatingSummary(
                                summary.getAverageRating(),
                                summary.getReviewCount() == null ? 0 : summary.getReviewCount())));
    }

    private ItemMatch participantMatch(Long matchId, UserProfile viewer) {
        ItemMatch match = itemMatchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        boolean isParticipant = match.getUserA().getId().equals(viewer.getId())
                || match.getUserB().getId().equals(viewer.getId());
        if (!isParticipant) {
            throw new MatchNotFoundException(matchId);
        }
        return match;
    }

    private UserProfile viewerProfile(Long appUserId) {
        return userProfileRepository
                .findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated app user has no profile: " + appUserId));
    }
}
