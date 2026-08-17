package com.threadtrades.match;

import com.threadtrades.review.RatingSummary;
import com.threadtrades.review.ReviewService;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private final ItemMatchRepository itemMatchRepository;
    private final UserProfileRepository userProfileRepository;
    private final ReviewService reviewService;

    public MatchService(
            ItemMatchRepository itemMatchRepository,
            UserProfileRepository userProfileRepository,
            ReviewService reviewService) {
        this.itemMatchRepository = itemMatchRepository;
        this.userProfileRepository = userProfileRepository;
        this.reviewService = reviewService;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> listForUser(Long appUserId) {
        UserProfile viewer = userProfileRepository
                .findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated app user has no profile: " + appUserId));
        List<ItemMatch> matches =
                itemMatchRepository.findByUserAIdOrUserBIdOrderByCreatedAtDesc(viewer.getId(), viewer.getId());
        Map<Long, RatingSummary> ratings = reviewService.getRatingSummaries(matches.stream()
                .map(match -> otherUserId(match, viewer.getId()))
                .distinct()
                .toList());
        return matches.stream()
                .map(match -> {
                    RatingSummary rating = ratings.getOrDefault(otherUserId(match, viewer.getId()), RatingSummary.NONE);
                    return MatchResponse.from(match, viewer.getId(), rating.averageRating(), rating.reviewCount());
                })
                .toList();
    }

    private Long otherUserId(ItemMatch match, Long viewerProfileId) {
        return match.getUserA().getId().equals(viewerProfileId) ? match.getUserB().getId() : match.getUserA().getId();
    }
}
