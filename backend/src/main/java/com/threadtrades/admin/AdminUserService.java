package com.threadtrades.admin;

import com.threadtrades.common.PagedResponse;
import com.threadtrades.user.AppUser;
import com.threadtrades.user.AppUserRepository;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileNotFoundException;
import com.threadtrades.user.UserProfileRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserProfileRepository userProfileRepository;
    private final AppUserRepository appUserRepository;

    public AdminUserService(UserProfileRepository userProfileRepository, AppUserRepository appUserRepository) {
        this.userProfileRepository = userProfileRepository;
        this.appUserRepository = appUserRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminUserResponse> list(int page, int size, String query) {
        String normalizedQuery = (query == null) ? "" : query.trim();
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PagedResponse.from(
                userProfileRepository.searchForAdmin(normalizedQuery, pageable).map(AdminUserResponse::from));
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(Long profileId) {
        return AdminUserResponse.from(profileFor(profileId));
    }

    @Transactional
    public AdminUserResponse ban(Long profileId, Long actingAppUserId) {
        UserProfile profile = profileFor(profileId);
        AppUser appUser = profile.getAppUser();
        if (appUser.getId().equals(actingAppUserId)) {
            throw new CannotBanSelfException();
        }
        appUser.ban();
        appUserRepository.save(appUser);
        return AdminUserResponse.from(profile);
    }

    @Transactional
    public AdminUserResponse unban(Long profileId) {
        UserProfile profile = profileFor(profileId);
        profile.getAppUser().unban();
        appUserRepository.save(profile.getAppUser());
        return AdminUserResponse.from(profile);
    }

    private UserProfile profileFor(Long profileId) {
        return userProfileRepository.findById(profileId).orElseThrow(() -> new UserProfileNotFoundException(profileId));
    }
}
