package com.threadtrades.swap;

import com.threadtrades.clothing.ClothingItemResponse;
import com.threadtrades.match.ItemMatch;
import com.threadtrades.user.UserProfile;
import java.time.Instant;
import java.util.List;

public record SwapHistoryResponse(
        Long matchId,
        Long otherUserId,
        String otherUsername,
        String otherUserName,
        String otherUserProfilePictureUrl,
        ClothingItemResponse myItem,
        ClothingItemResponse otherItem,
        List<SwapExtraItemResponse> myExtraItems,
        List<SwapExtraItemResponse> otherExtraItems,
        Long cashAmountPence,
        Boolean iOweCash,
        Instant completedAt) {

    static SwapHistoryResponse from(Swap swap, Long viewerProfileId, List<SwapExtraItem> extraItems) {
        ItemMatch match = swap.getMatch();
        boolean viewerIsA = match.getUserA().getId().equals(viewerProfileId);
        TradeSide viewerSide = viewerIsA ? TradeSide.A : TradeSide.B;
        UserProfile otherUser = viewerIsA ? match.getUserB() : match.getUserA();
        List<SwapExtraItemResponse> myExtraItems = extraItems.stream()
                .filter(extraItem -> extraItem.getSide() == viewerSide)
                .map(extraItem -> SwapExtraItemResponse.from(extraItem, viewerSide))
                .toList();
        List<SwapExtraItemResponse> otherExtraItems = extraItems.stream()
                .filter(extraItem -> extraItem.getSide() != viewerSide)
                .map(extraItem -> SwapExtraItemResponse.from(extraItem, viewerSide))
                .toList();
        Boolean iOweCash = swap.hasCashTerm() ? swap.getCashOwedBySide() == viewerSide : null;
        return new SwapHistoryResponse(
                match.getId(),
                otherUser.getId(),
                otherUser.getUsername(),
                otherUser.getName(),
                otherUser.getProfilePictureUrl(),
                ClothingItemResponse.from(viewerIsA ? match.getItemA() : match.getItemB()),
                ClothingItemResponse.from(viewerIsA ? match.getItemB() : match.getItemA()),
                myExtraItems,
                otherExtraItems,
                swap.getCashAmountPence(),
                iOweCash,
                swap.getUpdatedAt());
    }
}
