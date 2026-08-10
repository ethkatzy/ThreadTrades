package com.threadtrades.user;

import com.threadtrades.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserProfileRepository userProfileRepository;

    public UserController(UserProfileRepository userProfileRepository) {
        this.userProfileRepository = userProfileRepository;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        UserProfile profile = userProfileRepository
                .findByAppUserId(currentUser.appUserId())
                .orElseThrow(() -> new IllegalStateException(
                        "Authenticated app user has no profile: " + currentUser.appUserId()));
        return UserProfileResponse.from(profile);
    }
}
