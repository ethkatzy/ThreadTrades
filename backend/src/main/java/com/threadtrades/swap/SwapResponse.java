package com.threadtrades.swap;

import java.time.Instant;

public record SwapResponse(
        Long id, Long matchId, SwapStatus status, boolean iAccepted, boolean otherAccepted, Instant updatedAt) {

    static SwapResponse from(Swap swap, Long viewerProfileId) {
        boolean viewerIsA = swap.getMatch().getUserA().getId().equals(viewerProfileId);
        boolean iAccepted = viewerIsA ? swap.isAcceptedByUserA() : swap.isAcceptedByUserB();
        boolean otherAccepted = viewerIsA ? swap.isAcceptedByUserB() : swap.isAcceptedByUserA();
        return new SwapResponse(
                swap.getId(), swap.getMatch().getId(), swap.getStatus(), iAccepted, otherAccepted, swap.getUpdatedAt());
    }
}
