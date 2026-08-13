package com.threadtrades.message;

import java.time.Instant;

public record MessageResponse(Long id, Long matchId, Long senderId, String content, Instant sentAt) {

    static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getMatch().getId(),
                message.getSender().getId(),
                message.getContent(),
                message.getSentAt());
    }
}
