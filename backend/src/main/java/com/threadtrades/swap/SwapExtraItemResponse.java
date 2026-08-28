package com.threadtrades.swap;

import com.threadtrades.clothing.ClothingItemResponse;
import java.time.Instant;

public record SwapExtraItemResponse(ClothingItemResponse item, Instant addedAt, boolean proposedByMe) {

    static SwapExtraItemResponse from(SwapExtraItem extraItem, TradeSide viewerSide) {
        return new SwapExtraItemResponse(
                ClothingItemResponse.from(extraItem.getClothingItem()),
                extraItem.getCreatedAt(),
                extraItem.getProposedBySide() == viewerSide);
    }
}
