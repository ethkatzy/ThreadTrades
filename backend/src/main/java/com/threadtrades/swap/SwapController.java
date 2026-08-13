package com.threadtrades.swap;

import com.threadtrades.security.AuthenticatedUser;
import com.threadtrades.ws.SwapTopics;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches/{matchId}/swap")
public class SwapController {

    private final SwapService swapService;
    private final SimpMessagingTemplate messagingTemplate;

    public SwapController(SwapService swapService, SimpMessagingTemplate messagingTemplate) {
        this.swapService = swapService;
        this.messagingTemplate = messagingTemplate;
    }

    @GetMapping
    public SwapResponse get(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long matchId) {
        return swapService.getSwap(currentUser.appUserId(), matchId);
    }

    @PatchMapping("/accept")
    public SwapResponse accept(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long matchId) {
        // Broadcast after swapService's @Transactional method has returned (i.e. committed),
        // matching MessageController's pattern for the same reason.
        SwapResponse response = swapService.accept(currentUser.appUserId(), matchId);
        messagingTemplate.convertAndSend(SwapTopics.destination(matchId), response);
        return response;
    }

    @PatchMapping("/reject")
    public SwapResponse reject(@AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long matchId) {
        SwapResponse response = swapService.reject(currentUser.appUserId(), matchId);
        messagingTemplate.convertAndSend(SwapTopics.destination(matchId), response);
        return response;
    }
}
