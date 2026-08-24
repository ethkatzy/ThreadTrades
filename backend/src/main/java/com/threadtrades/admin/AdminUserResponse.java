package com.threadtrades.admin;

import com.threadtrades.user.AppUser;
import com.threadtrades.user.Role;
import com.threadtrades.user.UserProfile;
import java.time.Instant;

public record AdminUserResponse(
        Long id,
        Long appUserId,
        String email,
        String username,
        String name,
        Role role,
        boolean banned,
        Instant createdAt) {

    static AdminUserResponse from(UserProfile profile) {
        AppUser appUser = profile.getAppUser();
        return new AdminUserResponse(
                profile.getId(),
                appUser.getId(),
                appUser.getEmail(),
                profile.getUsername(),
                profile.getName(),
                appUser.getRole(),
                appUser.isBanned(),
                profile.getCreatedAt());
    }
}
