package com.threadtrades.user;

public record UserProfileResponse(
        Long id, String username, String name, String bio, String profilePictureUrl, Role role) {

    static UserProfileResponse from(UserProfile profile) {
        return from(profile, null);
    }

    /**
     * Role comes from the caller (the authenticated principal's already-verified
     * JWT claim, per UserController), not a fresh DB read -- avoids a lazy-load
     * of UserProfile.appUser outside the owning transaction.
     */
    static UserProfileResponse from(UserProfile profile, Role role) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUsername(),
                profile.getName(),
                profile.getBio(),
                profile.getProfilePictureUrl(),
                role);
    }
}
