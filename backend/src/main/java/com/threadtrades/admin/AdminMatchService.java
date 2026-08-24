package com.threadtrades.admin;

import com.threadtrades.common.PagedResponse;
import com.threadtrades.match.ItemMatch;
import com.threadtrades.match.ItemMatchRepository;
import com.threadtrades.match.MatchNotFoundException;
import com.threadtrades.swap.SwapRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminMatchService {

    private final ItemMatchRepository itemMatchRepository;
    private final SwapRepository swapRepository;

    public AdminMatchService(ItemMatchRepository itemMatchRepository, SwapRepository swapRepository) {
        this.itemMatchRepository = itemMatchRepository;
        this.swapRepository = swapRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminMatchResponse> list(int page, int size) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return PagedResponse.from(itemMatchRepository
                .findAll(pageable)
                .map(match -> AdminMatchResponse.from(
                        match, swapRepository.findByMatchId(match.getId()).orElse(null))));
    }

    @Transactional(readOnly = true)
    public AdminMatchResponse get(Long id) {
        ItemMatch match = itemMatchRepository.findById(id).orElseThrow(() -> new MatchNotFoundException(id));
        return AdminMatchResponse.from(match, swapRepository.findByMatchId(id).orElse(null));
    }
}
