package com.threadtrades.swipe;

import jakarta.validation.constraints.NotNull;

record RecordSwipeRequest(@NotNull Long offeredItemId, @NotNull Long clothingItemId, @NotNull SwipeDecision decision) {
}
