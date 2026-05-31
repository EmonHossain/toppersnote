package com.sharenote.note;

import com.sharenote.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.Instant;

@Entity
@Table(name = "note_edit_proposals")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteEditProposal {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "note_id", nullable = false)
    private Note note;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "proposer_user_id", nullable = false)
    private User proposer;

    @Column(nullable = false, length = 1000)
    private String changeSummary;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 255)
    private String storedFileName;

    @Column(length = 64)
    private String fileHash;

    @Column(nullable = false, length = 150)
    private String contentType;

    @Column(nullable = false)
    private long fileSize;

    @Column(nullable = false, length = 1000)
    private String storagePath;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProposalStatus status = ProposalStatus.PENDING;

    @Column(length = 1000)
    private String rejectionReason;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_user_id")
    private User reviewedBy;

    public NoteEditProposal(
            Note note,
            User proposer,
            String changeSummary,
            String originalFileName,
            String storedFileName,
            String contentType,
            long fileSize,
            String storagePath,
            Instant createdAt
    ) {
        this.note = note;
        this.proposer = proposer;
        this.changeSummary = changeSummary;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.createdAt = createdAt;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }

    public void approve(User reviewer, Instant reviewedAt) {
        this.status = ProposalStatus.APPROVED;
        this.reviewedBy = reviewer;
        this.reviewedAt = reviewedAt;
    }

    public void reject(User reviewer, String rejectionReason, Instant reviewedAt) {
        this.status = ProposalStatus.REJECTED;
        this.rejectionReason = rejectionReason;
        this.reviewedBy = reviewer;
        this.reviewedAt = reviewedAt;
    }
}
