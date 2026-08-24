package com.threadtrades.admin;

import com.threadtrades.swap.Swap;
import com.threadtrades.swap.SwapStatus;
import java.time.Instant;

public record AdminSwapResponse(
        Long id,
        Long matchId,
        SwapStatus status,
        boolean acceptedByUserA,
        boolean acceptedByUserB,
        Instant createdAt,
        Instant updatedAt) {

    static AdminSwapResponse from(Swap swap) {
        return new AdminSwapResponse(
                swap.getId(),
                swap.getMatch().getId(),
                swap.getStatus(),
                swap.isAcceptedByUserA(),
                swap.isAcceptedByUserB(),
                swap.getCreatedAt(),
                swap.getUpdatedAt());
    }
}
