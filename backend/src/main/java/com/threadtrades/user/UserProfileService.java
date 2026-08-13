package com.threadtrades.user;

import com.threadtrades.match.ItemMatch;
import com.threadtrades.match.ItemMatchRepository;
import com.threadtrades.match.MatchNotFoundException;
import com.threadtrades.storage.StorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class UserProfileService {

    private final UserProfileRepository userProfileRepository;
    private final ItemMatchRepository itemMatchRepository;
    private final StorageService storageService;

    public UserProfileService(
            UserProfileRepository userProfileRepository,
            ItemMatchRepository itemMatchRepository,
            StorageService storageService) {
        this.userProfileRepository = userProfileRepository;
        this.itemMatchRepository = itemMatchRepository;
        this.storageService = storageService;
    }

    @Transactional(readOnly = true)
    public UserProfile getProfile(Long appUserId) {
        return profileFor(appUserId);
    }

    /**
     * The other participant's profile for a given match -- scoped to matches
     * the viewer is actually part of, so profile details aren't a public
     * lookup-by-id. 404 rather than 403 for non-participants, same reasoning
     * as MessageService/SwapService's participant checks.
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getMatchedUserProfile(Long appUserId, Long matchId) {
        UserProfile viewer = profileFor(appUserId);
        ItemMatch match = itemMatchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        boolean viewerIsA = match.getUserA().getId().equals(viewer.getId());
        boolean viewerIsB = match.getUserB().getId().equals(viewer.getId());
        if (!viewerIsA && !viewerIsB) {
            throw new MatchNotFoundException(matchId);
        }
        return UserProfileResponse.from(viewerIsA ? match.getUserB() : match.getUserA());
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
