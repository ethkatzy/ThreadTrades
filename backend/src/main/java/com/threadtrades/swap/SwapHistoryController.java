package com.threadtrades.swap;

import com.threadtrades.security.AuthenticatedUser;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/swaps")
public class SwapHistoryController {

    private final SwapService swapService;

    public SwapHistoryController(SwapService swapService) {
        this.swapService = swapService;
    }

    @GetMapping("/history")
    public List<SwapHistoryResponse> history(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return swapService.listHistoryForUser(currentUser.appUserId());
    }
}
