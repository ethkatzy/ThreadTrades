package com.threadtrades.message;

import com.threadtrades.match.ItemMatch;
import com.threadtrades.match.ItemMatchRepository;
import com.threadtrades.match.MatchNotFoundException;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MessageService {

    private final MessageRepository messageRepository;
    private final ItemMatchRepository itemMatchRepository;
    private final UserProfileRepository userProfileRepository;

    public MessageService(
            MessageRepository messageRepository,
            ItemMatchRepository itemMatchRepository,
            UserProfileRepository userProfileRepository) {
        this.messageRepository = messageRepository;
        this.itemMatchRepository = itemMatchRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional(readOnly = true)
    public List<Message> listMessages(Long appUserId, Long matchId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        return messageRepository.findByMatchIdOrderBySentAtAsc(match.getId());
    }

    @Transactional
    public Message sendMessage(Long appUserId, Long matchId, String content) {
        UserProfile sender = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, sender);
        return messageRepository.save(new Message(match, sender, content));
    }

    private ItemMatch participantMatch(Long matchId, UserProfile viewer) {
        ItemMatch match = itemMatchRepository.findById(matchId).orElseThrow(() -> new MatchNotFoundException(matchId));
        boolean isParticipant = match.getUserA().getId().equals(viewer.getId())
                || match.getUserB().getId().equals(viewer.getId());
        // 404 rather than 403 for non-participants too, so a stranger probing match
        // ids can't distinguish "doesn't exist" from "exists but isn't yours".
        if (!isParticipant) {
            throw new MatchNotFoundException(matchId);
        }
        return match;
    }

    private UserProfile viewerProfile(Long appUserId) {
        return userProfileRepository
                .findByAppUserId(appUserId)
                .orElseThrow(() -> new IllegalStateException("Authenticated app user has no profile: " + appUserId));
    }
}
