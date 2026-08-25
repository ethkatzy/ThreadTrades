package com.threadtrades.ws;

import com.threadtrades.match.ItemMatch;
import com.threadtrades.match.ItemMatchRepository;
import com.threadtrades.security.AuthenticatedUser;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP CONNECT frames using the session cookie read off the
 * raw WS handshake request by SessionCookieHandshakeInterceptor (see that
 * class -- STOMP frames themselves carry no cookie, only the underlying
 * WebSocket session attributes do), then authorizes SUBSCRIBE frames so a
 * client can only listen on a match they're actually part of.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final ItemMatchRepository itemMatchRepository;
    private final UserProfileRepository userProfileRepository;

    public StompAuthChannelInterceptor(
            ItemMatchRepository itemMatchRepository, UserProfileRepository userProfileRepository) {
        this.itemMatchRepository = itemMatchRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }
        switch (accessor.getCommand()) {
            case CONNECT -> authenticate(accessor);
            case SUBSCRIBE -> authorizeSubscription(accessor);
            default -> {
            }
        }
        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        // Populated by SessionCookieHandshakeInterceptor from the session cookie on the raw
        // WS handshake request -- STOMP CONNECT frames carry no cookie of their own.
        Object attribute = accessor.getSessionAttributes() != null
                ? accessor.getSessionAttributes().get(WsSessionAttributes.AUTHENTICATED_USER)
                : null;
        if (!(attribute instanceof AuthenticatedUser user)) {
            throw new StompAuthenticationException("Missing or invalid token");
        }
        accessor.setUser(new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        Long matchId = MatchTopics.matchIdFromDestination(accessor.getDestination())
                .orElseThrow(() -> new StompAuthenticationException("Unknown destination"));
        AuthenticatedUser user = currentUser(accessor);
        UserProfile viewer = userProfileRepository
                .findByAppUserId(user.appUserId())
                .orElseThrow(() -> new StompAuthenticationException("Not authenticated"));
        ItemMatch match = itemMatchRepository
                .findById(matchId)
                .orElseThrow(() -> new StompAuthenticationException("Match not found"));
        boolean participant = match.getUserA().getId().equals(viewer.getId())
                || match.getUserB().getId().equals(viewer.getId());
        if (!participant) {
            throw new StompAuthenticationException("Not a participant in this match");
        }
    }

    private AuthenticatedUser currentUser(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof Authentication authentication
                && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user;
        }
        throw new StompAuthenticationException("Not authenticated");
    }
}
