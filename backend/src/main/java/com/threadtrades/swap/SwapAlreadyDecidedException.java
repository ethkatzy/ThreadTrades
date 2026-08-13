package com.threadtrades.swap;

public class SwapAlreadyDecidedException extends RuntimeException {

    public SwapAlreadyDecidedException(Long matchId) {
        super("Swap already decided for match: " + matchId);
    }
}
