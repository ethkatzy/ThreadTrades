package com.threadtrades.swap;

import com.threadtrades.clothing.ClothingItemResponse;
import com.threadtrades.match.ItemMatch;
import com.threadtrades.user.UserProfile;
import java.time.Instant;

public record SwapHistoryResponse(
        Long matchId,
        Long otherUserId,
        String otherUsername,
        String otherUserName,
        String otherUserProfilePictureUrl,
        ClothingItemResponse myItem,
        ClothingItemResponse otherItem,
        Instant completedAt) {

    static SwapHistoryResponse from(Swap swap, Long viewerProfileId) {
        ItemMatch match = swap.getMatch();
        boolean viewerIsA = match.getUserA().getId().equals(viewerProfileId);
        UserProfile otherUser = viewerIsA ? match.getUserB() : match.getUserA();
        return new SwapHistoryResponse(
                match.getId(),
                otherUser.getId(),
                otherUser.getUsername(),
                otherUser.getName(),
                otherUser.getProfilePictureUrl(),
                ClothingItemResponse.from(viewerIsA ? match.getItemA() : match.getItemB()),
                ClothingItemResponse.from(viewerIsA ? match.getItemB() : match.getItemA()),
                swap.getUpdatedAt());
    }
}
