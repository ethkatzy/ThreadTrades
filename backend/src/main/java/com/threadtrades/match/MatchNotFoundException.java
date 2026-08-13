package com.threadtrades.match;

public class MatchNotFoundException extends RuntimeException {

    public MatchNotFoundException(Long id) {
        super("Match not found: " + id);
    }
}
