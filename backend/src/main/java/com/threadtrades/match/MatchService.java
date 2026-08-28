package com.threadtrades.match;

import com.threadtrades.message.Message;
import com.threadtrades.message.MessageRepository;
import com.threadtrades.message.MessageResponse;
import com.threadtrades.review.RatingSummary;
import com.threadtrades.review.Review;
import com.threadtrades.review.ReviewRepository;
import com.threadtrades.review.ReviewService;
import com.threadtrades.swap.Swap;
import com.threadtrades.swap.SwapExtraItem;
import com.threadtrades.swap.SwapExtraItemRepository;
import com.threadtrades.swap.SwapRepository;
import com.threadtrades.swap.SwapResponse;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private final ItemMatchRepository itemMatchRepository;
    private final UserProfileRepository userProfileRepository;
    private final ReviewService reviewService;
    private final SwapRepository swapRepository;
    private final ReviewRepository reviewRepository;
    private final MessageRepository messageRepository;
    private final SwapExtraItemRepository swapExtraItemRepository;

    public MatchService(
            ItemMatchRepository itemMatchRepository,
            UserProfileRepository userProfileRepository,
            ReviewService reviewService,
            SwapRepository swapRepository,
            ReviewRepository reviewRepository,
            MessageRepository messageRepository,
            SwapExtraItemRepository swapExtraItemRepository) {
        this.itemMatchRepository = itemMatchRepository;
        this.userProfileRepository = userProfileRepository;
        this.reviewService = reviewService;
        this.swapRepository = swapRepository;
        this.reviewRepository = reviewRepository;
        this.messageRepository = messageRepository;
        this.swapExtraItemRepository = swapExtraItemRepository;
    }

    /**
     * Enriches every match with its swap and last message in a fixed number of
     * batched queries, rather than the caller (originally the homepage dashboard)
     * fetching {@code GET .../swap} and {@code GET .../messages} per match --
     * see ROADMAP.md §3 "N+1 fetch" note.
     */
    @Transactional
    public List<MatchResponse> listForUser(Long appUserId) {
        UserProfile viewer = userProfileRepository
                .findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated app user has no profile: " + appUserId));
        List<ItemMatch> matches =
                itemMatchRepository.findByUserAIdOrUserBIdOrderByCreatedAtDesc(viewer.getId(), viewer.getId());
        if (matches.isEmpty()) {
            return List.of();
        }
        List<Long> matchIds = matches.stream().map(ItemMatch::getId).toList();

        Map<Long, RatingSummary> ratings = reviewService.getRatingSummaries(matches.stream()
                .map(match -> otherUserId(match, viewer.getId()))
                .distinct()
                .toList());

        Map<Long, Swap> swapsByMatchId = loadOrCreateSwaps(matches, matchIds);
        Map<Long, Review> myReviewsBySwapId =
                reviewRepository
                        .findBySwapIdInAndReviewerId(
                                swapsByMatchId.values().stream().map(Swap::getId).toList(), viewer.getId())
                        .stream()
                        .collect(Collectors.toMap(review -> review.getSwap().getId(), Function.identity()));
        Map<Long, Message> lastMessageByMatchId = messageRepository.findLatestByMatchIdIn(matchIds).stream()
                .collect(Collectors.toMap(message -> message.getMatch().getId(), Function.identity()));
        Map<Long, List<SwapExtraItem>> extraItemsBySwapId = swapExtraItemRepository
                .findBySwapIdIn(swapsByMatchId.values().stream().map(Swap::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(extraItem -> extraItem.getSwap().getId()));

        return matches.stream()
                .map(match -> {
                    RatingSummary rating = ratings.getOrDefault(otherUserId(match, viewer.getId()), RatingSummary.NONE);
                    Swap swap = swapsByMatchId.get(match.getId());
                    SwapResponse swapResponse = swap == null
                            ? null
                            : toSwapResponse(
                                    swap,
                                    viewer,
                                    myReviewsBySwapId.get(swap.getId()),
                                    extraItemsBySwapId.getOrDefault(swap.getId(), List.of()));
                    Message lastMessage = lastMessageByMatchId.get(match.getId());
                    return MatchResponse.from(
                            match,
                            viewer.getId(),
                            rating.averageRating(),
                            rating.reviewCount(),
                            swapResponse,
                            lastMessage == null ? null : MessageResponse.from(lastMessage));
                })
                .toList();
    }

    private SwapResponse toSwapResponse(Swap swap, UserProfile viewer, Review myReview, List<SwapExtraItem> extraItems) {
        return SwapResponse.from(
                swap,
                viewer.getId(),
                myReview != null,
                myReview == null ? null : myReview.getRating(),
                myReview == null ? null : myReview.getComment(),
                myReview == null ? null : myReview.isAutoGenerated(),
                extraItems);
    }

    /**
     * Mirrors {@code SwapService.findOrCreateSwap}'s lazy-create-on-first-read
     * behavior (every match should get a swap row the first time anyone looks at
     * it), just batched across every match in the list instead of one at a time --
     * without this, a brand-new never-opened match would show {@code swap: null}
     * on the dashboard and be silently excluded from "needs your action" until
     * someone visited its chat page once. The DataIntegrityViolationException
     * fallback handles two concurrent first-loads racing on the unique match_id
     * constraint, same as the single-match path.
     */
    private Map<Long, Swap> loadOrCreateSwaps(List<ItemMatch> matches, List<Long> matchIds) {
        Map<Long, Swap> swapsByMatchId = swapRepository.findByMatchIdIn(matchIds).stream()
                .collect(Collectors.toMap(swap -> swap.getMatch().getId(), Function.identity()));
        List<ItemMatch> missing =
                matches.stream().filter(match -> !swapsByMatchId.containsKey(match.getId())).toList();
        if (missing.isEmpty()) {
            return swapsByMatchId;
        }
        try {
            for (Swap swap : swapRepository.saveAll(missing.stream().map(Swap::new).toList())) {
                swapsByMatchId.put(swap.getMatch().getId(), swap);
            }
        } catch (DataIntegrityViolationException e) {
            List<Long> missingIds = missing.stream().map(ItemMatch::getId).toList();
            for (Swap swap : swapRepository.findByMatchIdIn(missingIds)) {
                swapsByMatchId.put(swap.getMatch().getId(), swap);
            }
        }
        return swapsByMatchId;
    }

    private Long otherUserId(ItemMatch match, Long viewerProfileId) {
        return match.getUserA().getId().equals(viewerProfileId) ? match.getUserB().getId() : match.getUserA().getId();
    }
}
