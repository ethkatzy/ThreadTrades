package com.threadtrades.clothing;

import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClothingItemRepository extends JpaRepository<ClothingItem, Long> {

    List<ClothingItem> findAllByOwnerId(Long ownerUserProfileId);

    @Query("""
            SELECT c FROM ClothingItem c
            WHERE c.owner.id <> :viewerProfileId
            AND c.id NOT IN (
                SELECT s.swipedItem.id FROM Swipe s
                WHERE s.swiper.id = :viewerProfileId AND s.offeredItem.id = :offeredItemId
            )
            ORDER BY c.lastAccessed DESC
            """)
    List<ClothingItem> findDeckFor(
            @Param("viewerProfileId") Long viewerProfileId,
            @Param("offeredItemId") Long offeredItemId,
            Pageable pageable);
}
