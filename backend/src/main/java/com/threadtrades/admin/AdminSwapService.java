package com.threadtrades.admin;

import com.threadtrades.common.PagedResponse;
import com.threadtrades.swap.Swap;
import com.threadtrades.swap.SwapExtraItemRepository;
import com.threadtrades.swap.SwapNotFoundException;
import com.threadtrades.swap.SwapRepository;
import com.threadtrades.swap.SwapStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminSwapService {

    private final SwapRepository swapRepository;
    private final SwapExtraItemRepository swapExtraItemRepository;

    public AdminSwapService(SwapRepository swapRepository, SwapExtraItemRepository swapExtraItemRepository) {
        this.swapRepository = swapRepository;
        this.swapExtraItemRepository = swapExtraItemRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminSwapResponse> list(int page, int size, SwapStatus status) {
        // Ascending updatedAt when filtering by status surfaces the stalest PENDING
        // swaps first -- the practical "find stuck swaps" query this exists for.
        Sort sort = status == null
                ? Sort.by(Sort.Direction.DESC, "createdAt")
                : Sort.by(Sort.Direction.ASC, "updatedAt");
        var pageable = PageRequest.of(page, size, sort);
        var swaps = status == null ? swapRepository.findAll(pageable) : swapRepository.findByStatus(status, pageable);
        return PagedResponse.from(swaps.map(AdminSwapResponse::from));
    }

    @Transactional(readOnly = true)
    public AdminSwapResponse get(Long id) {
        return AdminSwapResponse.from(swapFor(id));
    }

    /**
     * Admin override: unlike the user-facing accept/reject flow, this is allowed
     * on an already-decided swap too (e.g. to reverse a wrongly forced accept),
     * so it deliberately doesn't check swap.isPending() the way SwapService does.
     */
    @Transactional
    public AdminSwapResponse forceResolve(Long id, SwapStatus target) {
        Swap swap = swapFor(id);
        switch (target) {
            case ACCEPTED -> {
                swap.acceptAsUserA();
                swap.acceptAsUserB();
                swap.markAccepted();
                swap.getMatch().getItemA().markSwapped();
                swap.getMatch().getItemB().markSwapped();
                swapExtraItemRepository
                        .findBySwapId(swap.getId())
                        .forEach(extraItem -> extraItem.getClothingItem().markSwapped());
            }
            case REJECTED -> swap.markRejected();
            default -> throw new InvalidSwapForceResolveException();
        }
        return AdminSwapResponse.from(swapRepository.save(swap));
    }

    private Swap swapFor(Long id) {
        return swapRepository.findById(id).orElseThrow(() -> new SwapNotFoundException(id));
    }
}
