package com.threadtrades.clothing;

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
@Table(name = "clothing_item")
public class ClothingItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_user_profile_id", nullable = false)
    private UserProfile owner;

    @Column(nullable = false)
    private String name;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column
    private String brand;

    @Column(name = "item_type", nullable = false)
    private String itemType;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "clothing_size", nullable = false)
    private ClothingSize clothingSize;

    @Column
    private String colour;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Condition condition;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @CreationTimestamp
    @Column(name = "last_accessed", nullable = false, updatable = false)
    private Instant lastAccessed;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClothingItem() {
    }

    public ClothingItem(
            UserProfile owner,
            String name,
            String imageUrl,
            String brand,
            String itemType,
            String description,
            ClothingSize clothingSize,
            String colour,
            Condition condition,
            Gender gender) {
        this.owner = owner;
        this.name = name;
        this.imageUrl = imageUrl;
        this.brand = brand;
        this.itemType = itemType;
        this.description = description;
        this.clothingSize = clothingSize;
        this.colour = colour;
        this.condition = condition;
        this.gender = gender;
    }

    public Long getId() {
        return id;
    }

    public UserProfile getOwner() {
        return owner;
    }

    public String getName() {
        return name;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getBrand() {
        return brand;
    }

    public String getItemType() {
        return itemType;
    }

    public String getDescription() {
        return description;
    }

    public ClothingSize getClothingSize() {
        return clothingSize;
    }

    public String getColour() {
        return colour;
    }

    public Condition getCondition() {
        return condition;
    }

    public Gender getGender() {
        return gender;
    }

    public Instant getLastAccessed() {
        return lastAccessed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
