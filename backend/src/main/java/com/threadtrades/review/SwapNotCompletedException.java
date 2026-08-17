package com.threadtrades.review;

public class SwapNotCompletedException extends RuntimeException {

    public SwapNotCompletedException(Long matchId) {
        super("Swap for match " + matchId + " is not yet completed -- nothing to review");
    }
}
