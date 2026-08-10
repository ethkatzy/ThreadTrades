package com.threadtrades.swipe;

import com.threadtrades.match.ItemMatch;
import java.time.Instant;

public record SwipeResponse(
        Long id,
        Long offeredItemId,
        Long clothingItemId,
        SwipeDecision decision,
        Instant createdAt,
        boolean matched,
        Long matchId) {

    static SwipeResponse from(SwipeOutcome outcome) {
        Swipe swipe = outcome.swipe();
        ItemMatch match = outcome.match();
        return new SwipeResponse(
                swipe.getId(),
                swipe.getOfferedItem().getId(),
                swipe.getSwipedItem().getId(),
                swipe.getDecision(),
                swipe.getCreatedAt(),
                match != null,
                match != null ? match.getId() : null);
    }
}
