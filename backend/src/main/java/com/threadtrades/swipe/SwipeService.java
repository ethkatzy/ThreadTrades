package com.threadtrades.swipe;

import com.threadtrades.clothing.ClothingItem;
import com.threadtrades.clothing.ClothingItemNotFoundException;
import com.threadtrades.clothing.ClothingItemRepository;
import com.threadtrades.match.ItemMatch;
import com.threadtrades.match.ItemMatchRepository;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SwipeService {

    private final SwipeRepository swipeRepository;
    private final ClothingItemRepository clothingItemRepository;
    private final UserProfileRepository userProfileRepository;
    private final ItemMatchRepository itemMatchRepository;

    public SwipeService(
            SwipeRepository swipeRepository,
            ClothingItemRepository clothingItemRepository,
            UserProfileRepository userProfileRepository,
            ItemMatchRepository itemMatchRepository) {
        this.swipeRepository = swipeRepository;
        this.clothingItemRepository = clothingItemRepository;
        this.userProfileRepository = userProfileRepository;
        this.itemMatchRepository = itemMatchRepository;
    }

    @Transactional(readOnly = true)
    public List<ClothingItem> deck(Long appUserId, Long offeredItemId, int limit) {
        UserProfile viewer = viewerProfile(appUserId);
        ClothingItem offeredItem = ownedItem(viewer, offeredItemId);
        return clothingItemRepository.findDeckFor(viewer.getId(), offeredItem.getId(), PageRequest.of(0, limit));
    }

    @Transactional
    public SwipeOutcome recordSwipe(Long appUserId, Long offeredItemId, Long clothingItemId, SwipeDecision decision) {
        UserProfile swiper = viewerProfile(appUserId);
        ClothingItem offeredItem = ownedItem(swiper, offeredItemId);
        ClothingItem swipedItem = clothingItemRepository
                .findById(clothingItemId)
                .orElseThrow(() -> new ClothingItemNotFoundException(clothingItemId));

        if (swipedItem.getOwner().getId().equals(swiper.getId())) {
            throw new CannotSwipeOwnItemException(clothingItemId);
        }
        if (swipeRepository.existsBySwiperIdAndOfferedItemIdAndSwipedItemId(
                swiper.getId(), offeredItem.getId(), clothingItemId)) {
            throw new ItemAlreadySwipedException(clothingItemId);
        }

        Swipe swipe = swipeRepository.save(new Swipe(swiper, offeredItem, swipedItem, decision));
        ItemMatch match =
                decision == SwipeDecision.LIKE ? detectMatch(swiper, offeredItem, swipedItem) : null;
        return new SwipeOutcome(swipe, match);
    }

    /**
     * A match requires the exact reciprocal: the other user must have offered
     * {@code swipedItem} (their own item) and liked {@code offeredItem} (ours)
     * specifically -- liking any other item of ours doesn't count, since a
     * like is scoped to the item we offered it for.
     */
    private ItemMatch detectMatch(UserProfile swiper, ClothingItem offeredItem, ClothingItem swipedItem) {
        UserProfile otherUser = swipedItem.getOwner();
        boolean reciprocalLike = swipeRepository.existsBySwiperIdAndOfferedItemIdAndSwipedItemIdAndDecision(
                otherUser.getId(), swipedItem.getId(), offeredItem.getId(), SwipeDecision.LIKE);
        if (!reciprocalLike) {
            return null;
        }

        boolean swiperIsA = swiper.getId() < otherUser.getId();
        UserProfile userA = swiperIsA ? swiper : otherUser;
        ClothingItem itemA = swiperIsA ? offeredItem : swipedItem;
        UserProfile userB = swiperIsA ? otherUser : swiper;
        ClothingItem itemB = swiperIsA ? swipedItem : offeredItem;
        return itemMatchRepository.save(new ItemMatch(userA, itemA, userB, itemB));
    }

    private ClothingItem ownedItem(UserProfile owner, Long clothingItemId) {
        ClothingItem item = clothingItemRepository
                .findById(clothingItemId)
                .orElseThrow(() -> new ClothingItemNotFoundException(clothingItemId));
        if (!item.getOwner().getId().equals(owner.getId())) {
            throw new OfferedItemNotOwnedException(clothingItemId);
        }
        return item;
    }

    private UserProfile viewerProfile(Long appUserId) {
        return userProfileRepository
                .findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated app user has no profile: " + appUserId));
    }
}
