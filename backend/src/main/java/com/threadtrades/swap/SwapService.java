package com.threadtrades.swap;

import com.threadtrades.clothing.ClothingItem;
import com.threadtrades.clothing.ClothingItemNotFoundException;
import com.threadtrades.clothing.ClothingItemRepository;
import com.threadtrades.clothing.ClothingItemResponse;
import com.threadtrades.clothing.ItemStatus;
import com.threadtrades.match.ItemMatch;
import com.threadtrades.match.ItemMatchRepository;
import com.threadtrades.match.MatchNotFoundException;
import com.threadtrades.review.Review;
import com.threadtrades.review.ReviewRepository;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SwapService {

    private final SwapRepository swapRepository;
    private final ItemMatchRepository itemMatchRepository;
    private final UserProfileRepository userProfileRepository;
    private final ReviewRepository reviewRepository;
    private final ClothingItemRepository clothingItemRepository;
    private final SwapExtraItemRepository swapExtraItemRepository;

    public SwapService(
            SwapRepository swapRepository,
            ItemMatchRepository itemMatchRepository,
            UserProfileRepository userProfileRepository,
            ReviewRepository reviewRepository,
            ClothingItemRepository clothingItemRepository,
            SwapExtraItemRepository swapExtraItemRepository) {
        this.swapRepository = swapRepository;
        this.itemMatchRepository = itemMatchRepository;
        this.userProfileRepository = userProfileRepository;
        this.reviewRepository = reviewRepository;
        this.clothingItemRepository = clothingItemRepository;
        this.swapExtraItemRepository = swapExtraItemRepository;
    }

    @Transactional
    public SwapResponse getSwap(Long appUserId, Long matchId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = findOrCreateSwap(match);
        return toResponse(swap, viewer);
    }

    @Transactional
    public SwapResponse accept(Long appUserId, Long matchId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = findOrCreateSwap(match);
        if (!swap.isPending()) {
            throw new SwapAlreadyDecidedException(matchId);
        }

        boolean viewerIsA = match.getUserA().getId().equals(viewer.getId());
        if (viewerIsA) {
            swap.acceptAsUserA();
        } else {
            swap.acceptAsUserB();
        }
        if (swap.isBothAccepted()) {
            List<SwapExtraItem> extraItems = swapExtraItemRepository.findBySwapId(swap.getId());
            for (SwapExtraItem extraItem : extraItems) {
                if (extraItem.getClothingItem().getStatus() != ItemStatus.AVAILABLE) {
                    throw new ExtraItemNoLongerAvailableException(
                            extraItem.getClothingItem().getId());
                }
            }
            swap.markAccepted();
            match.getItemA().markSwapped();
            match.getItemB().markSwapped();
            extraItems.forEach(extraItem -> extraItem.getClothingItem().markSwapped());
        }
        swapRepository.save(swap);
        return toResponse(swap, viewer);
    }

    /**
     * Adds a clothing item to the negotiation, on top of the match's base item -- either
     * participant's own available item (an "offer") or the other participant's (a "request";
     * they still have to agree to it, same as any other term change). Which side of the trade
     * the item lands on is determined by who owns it, not who's calling; {@code proposedBySide}
     * separately records who actually added it, so the two can be told apart in responses.
     * Resets both sides' acceptance -- terms changed, so any prior accept is stale and both
     * sides must re-confirm.
     */
    @Transactional
    public SwapResponse addExtraItem(Long appUserId, Long matchId, Long clothingItemId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = findOrCreateSwap(match);
        if (!swap.isPending()) {
            throw new SwapAlreadyDecidedException(matchId);
        }

        ClothingItem item = clothingItemRepository
                .findById(clothingItemId)
                .orElseThrow(() -> new ClothingItemNotFoundException(clothingItemId));
        TradeSide ownerSide = sideOf(match, item.getOwner().getId())
                .orElseThrow(() -> new ExtraItemNotOwnedException(clothingItemId));
        if (item.getStatus() != ItemStatus.AVAILABLE) {
            throw new ExtraItemNotAvailableException(clothingItemId);
        }
        if (item.getId().equals(match.getItemA().getId()) || item.getId().equals(match.getItemB().getId())) {
            throw new ExtraItemAlreadyBaseItemException(clothingItemId);
        }
        if (swapExtraItemRepository.existsBySwapIdAndClothingItemId(swap.getId(), clothingItemId)) {
            throw new ExtraItemAlreadyAddedException(clothingItemId);
        }

        TradeSide proposedBySide = sideOf(match, viewer.getId()).orElseThrow();
        try {
            swapExtraItemRepository.saveAndFlush(new SwapExtraItem(swap, ownerSide, proposedBySide, item));
        } catch (DataIntegrityViolationException e) {
            throw new ExtraItemAlreadyAddedException(clothingItemId);
        }
        swap.resetAcceptance();
        swapRepository.save(swap);
        return toResponse(swap, viewer);
    }

    /**
     * Removes a previously-added extra item. Either participant may remove any extra item while
     * the swap is pending -- withdrawing your own offer, declining a request against your item,
     * or retracting a request you made are all the same operation, and none of them are
     * destructive (the item itself is untouched, and this just resets both sides' acceptance
     * like any other term change).
     */
    @Transactional
    public SwapResponse removeExtraItem(Long appUserId, Long matchId, Long clothingItemId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = findOrCreateSwap(match);
        if (!swap.isPending()) {
            throw new SwapAlreadyDecidedException(matchId);
        }

        SwapExtraItem extraItem = swapExtraItemRepository
                .findBySwapIdAndClothingItemId(swap.getId(), clothingItemId)
                .orElseThrow(() -> new ExtraItemNotFoundException(clothingItemId));

        swapExtraItemRepository.delete(extraItem);
        swap.resetAcceptance();
        swapRepository.save(swap);
        return toResponse(swap, viewer);
    }

    /**
     * The other participant's items available to request and add to the negotiation -- their
     * base item for this match is excluded (it's already part of the deal, not an addition).
     */
    @Transactional(readOnly = true)
    public List<ClothingItemResponse> listOtherUserItems(Long appUserId, Long matchId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        boolean viewerIsA = match.getUserA().getId().equals(viewer.getId());
        UserProfile otherUser = viewerIsA ? match.getUserB() : match.getUserA();
        Long otherBaseItemId = (viewerIsA ? match.getItemB() : match.getItemA()).getId();
        return clothingItemRepository.findAllByOwnerId(otherUser.getId()).stream()
                .filter(item -> item.getStatus() == ItemStatus.AVAILABLE && !item.getId().equals(otherBaseItemId))
                .map(ClothingItemResponse::from)
                .toList();
    }

    private Optional<TradeSide> sideOf(ItemMatch match, Long userProfileId) {
        if (match.getUserA().getId().equals(userProfileId)) {
            return Optional.of(TradeSide.A);
        }
        if (match.getUserB().getId().equals(userProfileId)) {
            return Optional.of(TradeSide.B);
        }
        return Optional.empty();
    }

    /**
     * Sets (replacing any prior value) the cash figure agreed to balance a value difference --
     * a plain tracked amount the users settle outside the app, never processed in-app. {@code
     * iOwe} is viewer-relative, translated to the underlying A/B side here so the client never
     * needs to know which side it is.
     */
    @Transactional
    public SwapResponse setCashTerm(Long appUserId, Long matchId, long amountPence, boolean iOwe) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = findOrCreateSwap(match);
        if (!swap.isPending()) {
            throw new SwapAlreadyDecidedException(matchId);
        }

        boolean viewerIsA = match.getUserA().getId().equals(viewer.getId());
        TradeSide viewerSide = viewerIsA ? TradeSide.A : TradeSide.B;
        TradeSide otherSide = viewerIsA ? TradeSide.B : TradeSide.A;
        swap.setCashTerm(iOwe ? viewerSide : otherSide, amountPence);
        swap.resetAcceptance();
        swapRepository.save(swap);
        return toResponse(swap, viewer);
    }

    @Transactional
    public SwapResponse clearCashTerm(Long appUserId, Long matchId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = findOrCreateSwap(match);
        if (!swap.isPending()) {
            throw new SwapAlreadyDecidedException(matchId);
        }

        swap.clearCashTerm();
        swap.resetAcceptance();
        swapRepository.save(swap);
        return toResponse(swap, viewer);
    }

    @Transactional
    public SwapResponse reject(Long appUserId, Long matchId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = findOrCreateSwap(match);
        if (!swap.isPending()) {
            throw new SwapAlreadyDecidedException(matchId);
        }

        swap.markRejected();
        swapRepository.save(swap);
        return toResponse(swap, viewer);
    }

    private SwapResponse toResponse(Swap swap, UserProfile viewer) {
        Optional<Review> myReview = reviewRepository.findBySwapIdAndReviewerId(swap.getId(), viewer.getId());
        List<SwapExtraItem> extraItems = swapExtraItemRepository.findBySwapId(swap.getId());
        return SwapResponse.from(
                swap,
                viewer.getId(),
                myReview.isPresent(),
                myReview.map(Review::getRating).orElse(null),
                myReview.map(Review::getComment).orElse(null),
                myReview.map(Review::isAutoGenerated).orElse(null),
                extraItems);
    }

    @Transactional(readOnly = true)
    public List<SwapHistoryResponse> listHistoryForUser(Long appUserId) {
        UserProfile viewer = viewerProfile(appUserId);
        List<Swap> swaps = swapRepository.findCompletedForUser(viewer.getId());
        Map<Long, List<SwapExtraItem>> extraItemsBySwapId = swapExtraItemRepository
                .findBySwapIdIn(swaps.stream().map(Swap::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(extraItem -> extraItem.getSwap().getId()));
        return swaps.stream()
                .map(swap -> SwapHistoryResponse.from(
                        swap, viewer.getId(), extraItemsBySwapId.getOrDefault(swap.getId(), List.of())))
                .toList();
    }

    /**
     * Every match should already have a swap row (created eagerly going forward,
     * backfilled for matches from before this feature existed -- see V3 migration),
     * but this stays defensive rather than 500ing on a straggler. The
     * save/re-fetch fallback handles the rare case of two concurrent first-touches
     * racing on the unique match_id constraint.
     */
    private Swap findOrCreateSwap(ItemMatch match) {
        return swapRepository.findByMatchId(match.getId()).orElseGet(() -> {
            try {
                return swapRepository.saveAndFlush(new Swap(match));
            } catch (DataIntegrityViolationException e) {
                return swapRepository.findByMatchId(match.getId()).orElseThrow(() -> e);
            }
        });
    }

    private ItemMatch participantMatch(Long matchId, UserProfile viewer) {
        ItemMatch match = itemMatchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        boolean isParticipant = match.getUserA().getId().equals(viewer.getId())
                || match.getUserB().getId().equals(viewer.getId());
        // 404 rather than 403 for non-participants too, so a stranger probing match
        // ids can't distinguish "doesn't exist" from "exists but isn't yours".
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
