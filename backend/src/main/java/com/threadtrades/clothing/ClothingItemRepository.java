package com.threadtrades.clothing;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {

    List<ClothingItem> findAllByOwnerId(Long ownerUserProfileId);
}
