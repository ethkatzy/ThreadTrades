package com.threadtrades.ws;

import com.threadtrades.match.ItemMatch;
import com.threadtrades.match.ItemMatchRepository;
import com.threadtrades.security.AuthenticatedUser;
import com.threadtrades.security.JwtService;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

/**
 * Authenticates STOMP CONNECT frames via the same JWT used for REST calls
 * (browsers can't set custom headers on the WebSocket handshake itself, so
 * the token travels as a STOMP header instead -- see SecurityConfig, which
 * permits the raw /ws handshake and leaves enforcement to this interceptor),
 * then authorizes SUBSCRIBE frames so a client can only listen on a match
 * they're actually part of.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final ItemMatchRepository itemMatchRepository;
    private final UserProfileRepository userProfileRepository;

    public StompAuthChannelInterceptor(
            JwtService jwtService, ItemMatchRepository itemMatchRepository, UserProfileRepository userProfileRepository) {
        this.jwtService = jwtService;
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
        String header = accessor.getFirstNativeHeader("Authorization");
        String token = header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
        AuthenticatedUser user = Optional.ofNullable(token)
                .flatMap(jwtService::parseToken)
                .orElseThrow(() -> new StompAuthenticationException("Missing or invalid token"));
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
