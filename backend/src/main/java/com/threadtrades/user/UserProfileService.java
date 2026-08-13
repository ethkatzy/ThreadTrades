package com.threadtrades.user;

import com.threadtrades.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final StorageService storageService;

    public UserProfileService(UserProfileRepository userProfileRepository, StorageService storageService) {
        this.userProfileRepository = userProfileRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(Long appUserId) {
        return profileFor(appUserId);
    }

    @Transactional
    public UserProfile updateProfile(Long appUserId, String name, String bio, MultipartFile image) {
        UserProfile profile = profileFor(appUserId);
        profile.updateDetails(name, (bio == null || bio.isBlank()) ? null : bio);
        if (image != null && !image.isEmpty()) {
            profile.updateProfilePicture(storageService.store(image));
        }
        return userProfileRepository.save(profile);
    }

    private UserProfile profileFor(Long appUserId) {
        return userProfileRepository
                .findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated app user has no profile: " + appUserId));
    }
}
