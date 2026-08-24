package com.threadtrades.admin;

import com.threadtrades.clothing.ClothingItemResponse;
import com.threadtrades.match.ItemMatch;
import com.threadtrades.swap.Swap;
import java.time.Instant;

public record AdminMatchResponse(
        Long id,
        Long userAId,
        String userAUsername,
        ClothingItemResponse itemA,
        Long userBId,
        String userBUsername,
        ClothingItemResponse itemB,
        Instant createdAt,
        Long swapId,
        String swapStatus) {

    static AdminMatchResponse from(ItemMatch match, Swap swap) {
        return new AdminMatchResponse(
                match.getId(),
                match.getUserA().getId(),
                match.getUserA().getUsername(),
                ClothingItemResponse.from(match.getItemA()),
                match.getUserB().getId(),
                match.getUserB().getUsername(),
                ClothingItemResponse.from(match.getItemB()),
                match.getCreatedAt(),
                swap == null ? null : swap.getId(),
                swap == null ? null : swap.getStatus().name());
    }
}
