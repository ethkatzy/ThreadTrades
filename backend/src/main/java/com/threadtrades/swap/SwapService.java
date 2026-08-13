package com.threadtrades.swap;

import com.threadtrades.match.ItemMatch;
import com.threadtrades.match.ItemMatchRepository;
import com.threadtrades.match.MatchNotFoundException;
import com.threadtrades.user.UserProfile;
import com.threadtrades.user.UserProfileRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SwapService {

    private final SwapRepository swapRepository;
    private final ItemMatchRepository itemMatchRepository;
    private final UserProfileRepository userProfileRepository;

    public SwapService(
            SwapRepository swapRepository,
            ItemMatchRepository itemMatchRepository,
            UserProfileRepository userProfileRepository) {
        this.swapRepository = swapRepository;
        this.itemMatchRepository = itemMatchRepository;
        this.userProfileRepository = userProfileRepository;
    }

    @Transactional
    public SwapResponse getSwap(Long appUserId, Long matchId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        return SwapResponse.from(findOrCreateSwap(match), viewer.getId());
    }

    @Transactional
    public SwapResponse accept(Long appUserId, Long matchId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = findOrCreateSwap(match);
        if (!swap.isPending()) {
            throw new SwapAlreadyDecidedException(matchId);
        }

        boolean viewerIsA = match.getUserA().getId().equals(viewer.getId());
        if (viewerIsA) {
            swap.acceptAsUserA();
        } else {
            swap.acceptAsUserB();
        }
        if (swap.isBothAccepted()) {
            swap.markAccepted();
            match.getItemA().markSwapped();
            match.getItemB().markSwapped();
        }
        swapRepository.save(swap);
        return SwapResponse.from(swap, viewer.getId());
    }

    @Transactional
    public SwapResponse reject(Long appUserId, Long matchId) {
        UserProfile viewer = viewerProfile(appUserId);
        ItemMatch match = participantMatch(matchId, viewer);
        Swap swap = findOrCreateSwap(match);
        if (!swap.isPending()) {
            throw new SwapAlreadyDecidedException(matchId);
        }

        swap.markRejected();
        swapRepository.save(swap);
        return SwapResponse.from(swap, viewer.getId());
    }

    /**
     * Every match should already have a swap row (created eagerly going forward,
     * backfilled for matches from before this feature existed -- see V3 migration),
     * but this stays defensive rather than 500ing on a straggler. The
     * save/re-fetch fallback handles the rare case of two concurrent first-touches
     * racing on the unique match_id constraint.
     */
    private Swap findOrCreateSwap(ItemMatch match) {
        return swapRepository.findByMatchId(match.getId()).orElseGet(() -> {
            try {
                return swapRepository.saveAndFlush(new Swap(match));
            } catch (DataIntegrityViolationException e) {
                return swapRepository.findByMatchId(match.getId()).orElseThrow(() -> e);
            }
        });
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
