package com.threadtrades.match;

import com.threadtrades.clothing.ClothingItemResponse;
import com.threadtrades.user.UserProfile;
import java.time.Instant;

public record MatchResponse(
        Long id,
        Long otherUserId,
        String otherUsername,
        String otherUserName,
        String otherUserProfilePictureUrl,
        ClothingItemResponse myItem,
        ClothingItemResponse otherItem,
        Instant createdAt) {

    static MatchResponse from(ItemMatch match, Long viewerProfileId) {
        boolean viewerIsA = match.getUserA().getId().equals(viewerProfileId);
        UserProfile otherUser = viewerIsA ? match.getUserB() : match.getUserA();
        return new MatchResponse(
                match.getId(),
                otherUser.getId(),
                otherUser.getUsername(),
                otherUser.getName(),
                otherUser.getProfilePictureUrl(),
                ClothingItemResponse.from(viewerIsA ? match.getItemA() : match.getItemB()),
                ClothingItemResponse.from(viewerIsA ? match.getItemB() : match.getItemA()),
                match.getCreatedAt());
    }
}
