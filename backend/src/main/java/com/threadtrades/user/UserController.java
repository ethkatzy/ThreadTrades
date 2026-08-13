package com.threadtrades.user;

import com.threadtrades.security.AuthenticatedUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/users")
@Validated
public class UserController {

    private final UserProfileService userProfileService;

    public UserController(UserProfileService userProfileService) {
        this.userProfileService = userProfileService;
    }

    @GetMapping("/me")
    public UserProfileResponse me(@AuthenticationPrincipal AuthenticatedUser currentUser) {
        return UserProfileResponse.from(userProfileService.getProfile(currentUser.appUserId()));
    }

    @PatchMapping(path = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UserProfileResponse updateMe(
            @AuthenticationPrincipal AuthenticatedUser currentUser,
            @RequestParam @NotBlank @Size(max = 255) String name,
            @RequestParam(required = false) @Size(max = 1000) String bio,
            @RequestPart(name = "image", required = false) MultipartFile image) {
        UserProfile profile = userProfileService.updateProfile(currentUser.appUserId(), name, bio, image);
        return UserProfileResponse.from(profile);
    }
}
