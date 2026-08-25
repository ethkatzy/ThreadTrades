package com.threadtrades.swap;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SwapRepository extends JpaRepository<Swap, Long> {

    Optional<Swap> findByMatchId(Long matchId);

    List<Swap> findByMatchIdIn(Collection<Long> matchIds);

    Page<Swap> findByStatus(SwapStatus status, Pageable pageable);

    @Query("select s from Swap s where s.status = com.threadtrades.swap.SwapStatus.ACCEPTED "
            + "and (s.match.userA.id = :profileId or s.match.userB.id = :profileId) "
            + "order by s.updatedAt desc")
    List<Swap> findCompletedForUser(@Param("profileId") Long profileId);
}
