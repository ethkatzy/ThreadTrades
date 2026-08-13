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

    /**
     * Extracts the match id from a match-scoped destination, whether it's the
     * bare message thread ({@code /topic/matches/5}) or a suffixed subtopic
     * like {@link SwapTopics}'s ({@code /topic/matches/5/swap}) -- both are
     * scoped to the same match and authorized the same way by
     * {@link StompAuthChannelInterceptor}.
     */
    public static Optional<Long> matchIdFromDestination(String destination) {
        if (destination == null || !destination.startsWith(PREFIX)) {
            return Optional.empty();
        }
        String remainder = destination.substring(PREFIX.length());
        int slashIndex = remainder.indexOf('/');
        String idPart = slashIndex >= 0 ? remainder.substring(0, slashIndex) : remainder;
        try {
            return Optional.of(Long.valueOf(idPart));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}
