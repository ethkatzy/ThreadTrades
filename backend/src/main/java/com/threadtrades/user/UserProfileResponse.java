package com.threadtrades.user;

public record UserProfileResponse(
        Long id, String username, String name, String bio, String profilePictureUrl) {

    static UserProfileResponse from(UserProfile profile) {
        return new UserProfileResponse(
                profile.getId(),
                profile.getUsername(),
                profile.getName(),
                profile.getBio(),
                profile.getProfilePictureUrl());
    }
}
