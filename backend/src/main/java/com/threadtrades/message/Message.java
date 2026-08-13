package com.threadtrades.message;

import com.threadtrades.match.ItemMatch;
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

@Entity
@Table(name = "message")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "match_id", nullable = false)
    private ItemMatch match;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_user_profile_id", nullable = false)
    private UserProfile sender;

    @Column(nullable = false)
    private String content;

    @CreationTimestamp
    @Column(name = "sent_at", nullable = false, updatable = false)
    private Instant sentAt;

    protected Message() {
    }

    public Message(ItemMatch match, UserProfile sender, String content) {
        this.match = match;
        this.sender = sender;
        this.content = content;
    }

    public Long getId() {
        return id;
    }

    public ItemMatch getMatch() {
        return match;
    }

    public UserProfile getSender() {
        return sender;
    }

    public String getContent() {
        return content;
    }

    public Instant getSentAt() {
        return sentAt;
    }
}
