package com.sharenote.note;

import com.sharenote.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(
        name = "note_take_a_look_suggestions",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_note_take_a_look_sender_recipient",
                columnNames = {"note_id", "suggested_by_user_id", "suggested_to_user_id"}
        )
)
public class NoteTakeALookSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suggested_by_user_id", nullable = false)
    private User suggestedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "suggested_to_user_id", nullable = false)
    private User suggestedTo;

    @Column(length = 500)
    private String message;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected NoteTakeALookSuggestion() {
    }

    public NoteTakeALookSuggestion(Note note, User suggestedBy, User suggestedTo, String message, Instant createdAt) {
        this.note = note;
        this.suggestedBy = suggestedBy;
        this.suggestedTo = suggestedTo;
        this.message = message;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Note getNote() {
        return note;
    }

    public User getSuggestedBy() {
        return suggestedBy;
    }

    public User getSuggestedTo() {
        return suggestedTo;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
