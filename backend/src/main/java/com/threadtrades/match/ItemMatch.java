package com.threadtrades.match;

import com.threadtrades.clothing.ClothingItem;
import com.threadtrades.user.UserProfile;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;

/**
 * A match pairs two users over one item each (the items each side offered).
 * Canonical ordering (userA.id &lt; userB.id) lets a mutual-swipe check look
 * for exactly one row regardless of who triggered the second swipe.
 */
@Entity
@Table(name = "item_match")
public class ItemMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_a_profile_id", nullable = false)
    private UserProfile userA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_a_id", nullable = false)
    private ClothingItem itemA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_b_profile_id", nullable = false)
    private UserProfile userB;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "item_b_id", nullable = false)
    private ClothingItem itemB;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ItemMatch() {
    }

    public ItemMatch(UserProfile userA, ClothingItem itemA, UserProfile userB, ClothingItem itemB) {
        this.userA = userA;
        this.itemA = itemA;
        this.userB = userB;
        this.itemB = itemB;
    }

    public Long getId() {
        return id;
    }

    public UserProfile getUserA() {
        return userA;
    }

    public ClothingItem getItemA() {
        return itemA;
    }

    public UserProfile getUserB() {
        return userB;
    }

    public ClothingItem getItemB() {
        return itemB;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
