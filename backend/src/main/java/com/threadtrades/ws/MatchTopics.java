package com.threadtrades.ws;

import java.util.Optional;

/** The STOMP destination convention for a match's live message thread. */
public final class MatchTopics {

    private static final String PREFIX = "/topic/matches/";

    private MatchTopics() {
    }

    public static String destination(Long matchId) {
        return PREFIX + matchId;
    }

    public static Optional<Long> matchIdFromDestination(String destination) {
        if (destination == null || !destination.startsWith(PREFIX)) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.valueOf(destination.substring(PREFIX.length())));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
