package com.threadtrades.match;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItemMatchRepository extends JpaRepository<ItemMatch, Long> {

    List<ItemMatch> findByUserAIdOrUserBIdOrderByCreatedAtDesc(Long userAId, Long userBId);
}
