package com.threadtrades.user;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUsername(String username);

    Optional<UserProfile> findByAppUserId(Long appUserId);

    // :query is always bound as a non-null String (empty string when unfiltered) --
    // never pass a raw null here. A parameter used ONLY behind an "IS NULL" branch
    // (with no other typed usage) leaves Hibernate/pgjdbc unable to infer its JDBC
    // type, which on this stack (Hibernate 7 + Postgres) resolved it as `bytea` and
    // broke every LOWER(...) call in the query with "function lower(bytea) does not
    // exist" -- see ROADMAP.md notes. An empty string naturally matches everything
    // via LIKE '%%', so the null-check branch isn't needed at all.
    @Query("""
            SELECT p FROM UserProfile p JOIN FETCH p.appUser a
            WHERE LOWER(p.username) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(a.email) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<UserProfile> searchForAdmin(@Param("query") String query, Pageable pageable);
}
