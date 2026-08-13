package com.threadtrades.ws;

/**
 * The STOMP destination convention for a match's swap-status updates. Lives
 * under the same {@code /topic/matches/{matchId}} prefix as {@link MatchTopics}
 * so {@link StompAuthChannelInterceptor}'s per-match subscription check covers
 * it too, just with a {@code /swap} suffix so it doesn't collide with the
 * message-thread payload shape on the bare match topic.
 */
public final class SwapTopics {

    private SwapTopics() {
    }

    public static String destination(Long matchId) {
        return MatchTopics.destination(matchId) + "/swap";
    }
}
