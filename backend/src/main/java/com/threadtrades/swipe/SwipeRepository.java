package com.threadtrades.swipe;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    boolean existsBySwiperIdAndOfferedItemIdAndSwipedItemId(
            Long swiperUserProfileId, Long offeredClothingItemId, Long swipedClothingItemId);

    boolean existsBySwiperIdAndOfferedItemIdAndSwipedItemIdAndDecision(
            Long swiperUserProfileId, Long offeredClothingItemId, Long swipedClothingItemId, SwipeDecision decision);
}
