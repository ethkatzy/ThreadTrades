package com.threadtrades.message;

import com.threadtrades.security.AuthenticatedUser;
import com.threadtrades.ws.MatchTopics;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches/{matchId}/messages")
public class MessageController {

    private final MessageService messageService;
    private final SimpMessagingTemplate messagingTemplate;

    public MessageController(MessageService messageService, SimpMessagingTemplate messagingTemplate) {
        this.messageService = messageService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping
    public List<MessageResponse> list(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long matchId) {
        return messageService.listMessages(currentUser.appUserId(), matchId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @PostMapping
    public ResponseEntity<MessageResponse> send(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @PathVariable Long matchId,
            @Valid @RequestBody SendMessageRequest request) {
        Message message = messageService.sendMessage(currentUser.appUserId(), matchId, request.content());
        MessageResponse response = MessageResponse.from(message);
        // Broadcast after messageService's @Transactional method has returned (i.e. committed),
        // so a subscriber that reacts to the push by re-fetching via GET never sees a 404/miss.
        messagingTemplate.convertAndSend(MatchTopics.destination(matchId), response);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
