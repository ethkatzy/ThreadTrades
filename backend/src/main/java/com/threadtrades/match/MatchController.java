package com.threadtrades.match;

import com.threadtrades.security.AuthenticatedUser;
import com.threadtrades.user.UserProfileResponse;
import com.threadtrades.user.UserProfileService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/matches")
public class MatchController {

    private final MatchService matchService;
    private final UserProfileService userProfileService;

    public MatchController(MatchService matchService, UserProfileService userProfileService) {
        this.matchService = matchService;
        this.userProfileService = userProfileService;
    }

    @GetMapping
    public List<MatchResponse> mine(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return matchService.listForUser(currentUser.appUserId());
    }

    @GetMapping("/{matchId}/other-user")
    public UserProfileResponse otherUser(
            @AuthenticationPrincipal AuthenticatedUser currentUser, @PathVariable Long matchId) {
        return userProfileService.getMatchedUserProfile(currentUser.appUserId(), matchId);
    }
}
