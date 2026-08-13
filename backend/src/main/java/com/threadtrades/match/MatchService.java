package com.threadtrades.match;

import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private final ItemMatchRepository itemMatchRepository;
    private final UserProfileRepository userProfileRepository;

    public MatchService(ItemMatchRepository itemMatchRepository, UserProfileRepository userProfileRepository) {
        this.itemMatchRepository = itemMatchRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> listForUser(Long appUserId) {
        UserProfile viewer = userProfileRepository
                .findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated app user has no profile: " + appUserId));
        return itemMatchRepository.findByUserAIdOrUserBIdOrderByCreatedAtDesc(viewer.getId(), viewer.getId()).stream()
                .map(match -> MatchResponse.from(match, viewer.getId()))
                .toList();
    }
}
