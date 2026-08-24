package com.threadtrades.admin;

import com.threadtrades.clothing.ClothingItem;
import com.threadtrades.clothing.ClothingItemNotFoundException;
import com.threadtrades.clothing.ClothingItemRepository;
import com.threadtrades.clothing.ClothingItemResponse;
import com.threadtrades.clothing.ItemStatus;
import com.threadtrades.common.PagedResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminClothingItemService {

    private final ClothingItemRepository clothingItemRepository;

    public AdminClothingItemService(ClothingItemRepository clothingItemRepository) {
        this.clothingItemRepository = clothingItemRepository;
    }

    @Transactional(readOnly = true)
    public PagedResponse<ClothingItemResponse> list(int page, int size, ItemStatus status) {
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var items = status == null
                ? clothingItemRepository.findAll(pageable)
                : clothingItemRepository.findByStatus(status, pageable);
        return PagedResponse.from(items.map(ClothingItemResponse::from));
    }

    @Transactional(readOnly = true)
    public ClothingItemResponse get(Long id) {
        return ClothingItemResponse.from(itemFor(id));
    }

    /** Status flag, not a hard delete -- see V7 migration comment for why. */
    @Transactional
    public ClothingItemResponse remove(Long id) {
        ClothingItem item = itemFor(id);
        item.markRemoved();
        return ClothingItemResponse.from(clothingItemRepository.save(item));
    }

    private ClothingItem itemFor(Long id) {
        return clothingItemRepository.findById(id).orElseThrow(() -> new ClothingItemNotFoundException(id));
    }
}
