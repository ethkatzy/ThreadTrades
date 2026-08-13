package com.threadtrades.message;

import com.threadtrades.security.AuthenticatedUser;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
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
        return ResponseEntity.status(HttpStatus.CREATED).body(MessageResponse.from(message));
    }
}
