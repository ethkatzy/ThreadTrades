package com.threadtrades.swap;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapRepository extends JpaRepository<Swap, Long> {

    Optional<Swap> findByMatchId(Long matchId);
}
