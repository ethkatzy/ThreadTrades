package com.threadtrades.review;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findBySwapIdAndReviewerId(Long swapId, Long reviewerId);

    List<Review> findByRevieweeIdOrderByCreatedAtDesc(Long revieweeId);

    @Query("select avg(r.rating) as averageRating, count(r) as reviewCount from Review r "
            + "where r.reviewee.id = :revieweeId")
    RatingSummary getRatingSummary(@Param("revieweeId") Long revieweeId);

    @Query("select r.reviewee.id as userId, avg(r.rating) as averageRating, count(r) as reviewCount "
            + "from Review r where r.reviewee.id in :revieweeIds group by r.reviewee.id")
    List<UserRatingSummary> getRatingSummaries(@Param("revieweeIds") Collection<Long> revieweeIds);

    interface RatingSummary {
        Double getAverageRating();

        Long getReviewCount();
    }

    interface UserRatingSummary {
        Long getUserId();

        Double getAverageRating();

        Long getReviewCount();
    }
}
