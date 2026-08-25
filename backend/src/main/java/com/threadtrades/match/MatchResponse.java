package com.threadtrades.match;

import com.threadtrades.clothing.ClothingItemResponse;
import com.threadtrades.message.MessageResponse;
import com.threadtrades.swap.SwapResponse;
import com.threadtrades.user.UserProfile;
import java.time.Instant;

public record MatchResponse(
        Long id,
        Long otherUserId,
        String otherUsername,
        String otherUserName,
        String otherUserProfilePictureUrl,
        Double otherUserAverageRating,
        long otherUserReviewCount,
        ClothingItemResponse myItem,
        ClothingItemResponse otherItem,
        Instant createdAt,
        SwapResponse swap,
        MessageResponse lastMessage) {

    static MatchResponse from(
            ItemMatch match,
            Long viewerProfileId,
            Double otherUserAverageRating,
            long otherUserReviewCount,
            SwapResponse swap,
            MessageResponse lastMessage) {
        boolean viewerIsA = match.getUserA().getId().equals(viewerProfileId);
        UserProfile otherUser = viewerIsA ? match.getUserB() : match.getUserA();
        return new MatchResponse(
                match.getId(),
                otherUser.getId(),
                otherUser.getUsername(),
                otherUser.getName(),
                otherUser.getProfilePictureUrl(),
                otherUserAverageRating,
                otherUserReviewCount,
                ClothingItemResponse.from(viewerIsA ? match.getItemA() : match.getItemB()),
                ClothingItemResponse.from(viewerIsA ? match.getItemB() : match.getItemA()),
                match.getCreatedAt(),
                swap,
                lastMessage);
    }
}
