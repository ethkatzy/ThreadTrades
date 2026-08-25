package com.threadtrades.message;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByMatchIdOrderBySentAtAsc(Long matchId);

    Page<Message> findByMatchId(Long matchId, Pageable pageable);

    /**
     * One row per match (the most recent message), via Postgres's {@code DISTINCT ON}
     * -- native SQL because JPQL has no equivalent, and a per-match "top 1" would
     * otherwise mean one query per match.
     */
    @Query(
            value = "select distinct on (m.match_id) m.* from message m "
                    + "where m.match_id in :matchIds order by m.match_id, m.sent_at desc",
            nativeQuery = true)
    List<Message> findLatestByMatchIdIn(@Param("matchIds") Collection<Long> matchIds);
}
