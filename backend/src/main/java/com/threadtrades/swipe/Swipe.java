package com.threadtrades.swipe;

import com.threadtrades.clothing.ClothingItem;
import com.threadtrades.user.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(name = "swipe")
public class Swipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swiper_user_profile_id", nullable = false)
    private UserProfile swiper;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "offered_clothing_item_id", nullable = false)
    private ClothingItem offeredItem;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swiped_clothing_item_id", nullable = false)
    private ClothingItem swipedItem;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SwipeDecision decision;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Swipe() {
    }

    public Swipe(UserProfile swiper, ClothingItem offeredItem, ClothingItem swipedItem, SwipeDecision decision) {
        this.swiper = swiper;
        this.offeredItem = offeredItem;
        this.swipedItem = swipedItem;
        this.decision = decision;
    }

    public Long getId() {
        return id;
    }

    public UserProfile getSwiper() {
        return swiper;
    }

    public ClothingItem getOfferedItem() {
        return offeredItem;
    }

    public ClothingItem getSwipedItem() {
        return swipedItem;
    }

    public SwipeDecision getDecision() {
        return decision;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
