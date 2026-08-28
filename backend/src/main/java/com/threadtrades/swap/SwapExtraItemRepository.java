package com.threadtrades.swap;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapExtraItemRepository extends JpaRepository<SwapExtraItem, Long> {

    List<SwapExtraItem> findBySwapId(Long swapId);

    List<SwapExtraItem> findBySwapIdIn(Collection<Long> swapIds);

    Optional<SwapExtraItem> findBySwapIdAndClothingItemId(Long swapId, Long clothingItemId);

    boolean existsBySwapIdAndClothingItemId(Long swapId, Long clothingItemId);
}
