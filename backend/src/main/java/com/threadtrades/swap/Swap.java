package com.threadtrades.swap;

import com.threadtrades.match.ItemMatch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * One row per match (unique {@code match_id}): real state transitions on this
 * single row, never a new row per accept/reject. Finalizing to ACCEPTED
 * requires both {@code acceptedByUserA} and {@code acceptedByUserB} -- a
 * single accept just records that side's consent and waits on the other.
 */
@Entity
@Table(name = "swap")
public class Swap {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false, unique = true)
    private ItemMatch match;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SwapStatus status = SwapStatus.PENDING;

    @Column(name = "accepted_by_user_a", nullable = false)
    private boolean acceptedByUserA;

    @Column(name = "accepted_by_user_b", nullable = false)
    private boolean acceptedByUserB;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Swap() {
    }

    public Swap(ItemMatch match) {
        this.match = match;
    }

    public Long getId() {
        return id;
    }

    public ItemMatch getMatch() {
        return match;
    }

    public SwapStatus getStatus() {
        return status;
    }

    public boolean isPending() {
        return status == SwapStatus.PENDING;
    }

    public boolean isAcceptedByUserA() {
        return acceptedByUserA;
    }

    public boolean isAcceptedByUserB() {
        return acceptedByUserB;
    }

    public void acceptAsUserA() {
        this.acceptedByUserA = true;
    }

    public void acceptAsUserB() {
        this.acceptedByUserB = true;
    }

    public boolean isBothAccepted() {
        return acceptedByUserA && acceptedByUserB;
    }

    public void markAccepted() {
        this.status = SwapStatus.ACCEPTED;
    }

    public void markRejected() {
        this.status = SwapStatus.REJECTED;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
