package com.threadtrades.swap;

import com.threadtrades.clothing.ClothingItem;
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

/**
 * One item on top of the match's base item that's part of the negotiated deal, to balance a
 * value difference. {@code side} is whichever side *owns* the item; {@code proposedBySide} is
 * whichever side *added* it -- they're the same when a participant offers their own item, and
 * different when a participant requests an item from the other side. Not held as a collection
 * on {@link Swap} -- fetched via {@link SwapExtraItemRepository} and joined at the service layer,
 * matching this codebase's convention for related rows.
 */
@Entity
@Table(name = "swap_extra_item")
public class SwapExtraItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "swap_id", nullable = false)
    private Swap swap;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeSide side;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposed_by_side", nullable = false)
    private TradeSide proposedBySide;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clothing_item_id", nullable = false)
    private ClothingItem clothingItem;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected SwapExtraItem() {
    }

    public SwapExtraItem(Swap swap, TradeSide side, TradeSide proposedBySide, ClothingItem clothingItem) {
        this.swap = swap;
        this.side = side;
        this.proposedBySide = proposedBySide;
        this.clothingItem = clothingItem;
    }

    public Long getId() {
        return id;
    }

    public Swap getSwap() {
        return swap;
    }

    public TradeSide getSide() {
        return side;
    }

    public TradeSide getProposedBySide() {
        return proposedBySide;
    }

    public ClothingItem getClothingItem() {
        return clothingItem;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
