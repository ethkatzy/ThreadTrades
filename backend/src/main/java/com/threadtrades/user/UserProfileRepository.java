package com.threadtrades.user;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserProfileRepository extends JpaRepository<UserProfile, Long> {

    boolean existsByUsername(String username);

    Optional<UserProfile> findByAppUserId(Long appUserId);
}
