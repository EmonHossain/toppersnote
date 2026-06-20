package com.sharenote.admin.retention;

import com.sharenote.note.Note;
import com.sharenote.user.User;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "note_retention_candidates")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NoteRetentionCandidate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "note_id")
    private Note note;

    @Column(name = "note_id_snapshot", nullable = false)
    private Long noteIdSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, length = 255)
    private String originalFileName;

    @Column(nullable = false, length = 120)
    private String subjectClass;

    @Column(nullable = false, length = 120)
    private String institution;

    @Column(nullable = false, length = 120)
    private String degreeProgram;

    @Column(nullable = false, length = 50)
    private String semester;

    @Column(nullable = false, length = 20)
    private String year;

    @Column(nullable = false)
    private Instant uploadedAt;

    @Column(nullable = false)
    private Instant noticeDueAt;

    @Column(nullable = false)
    private Instant removalDueAt;

    private Instant noticeSentAt;

    private Instant removedAt;

    private Instant cancelledAt;

    @Column(length = 500)
    private String cancelReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NoteRetentionStatus status = NoteRetentionStatus.PENDING_NOTICE;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    // NoteRetentionCandidate: Creates a scheduled auto-removal candidate from a note snapshot.
    public NoteRetentionCandidate(Note note, Instant noticeDueAt, Instant removalDueAt, Instant createdAt) {
        this.note = note;
        this.noteIdSnapshot = note.getId();
        this.uploadedBy = note.getUploadedBy();
        this.originalFileName = note.getOriginalFileName();
        this.subjectClass = note.getSubjectClass();
        this.institution = note.getInstitution();
        this.degreeProgram = note.getDegreeProgram();
        this.semester = note.getSemester();
        this.year = note.getYear();
        this.uploadedAt = note.getCreatedAt();
        this.noticeDueAt = noticeDueAt;
        this.removalDueAt = removalDueAt;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    // markNoticeSent: Records that the uploader warning was sent.
    public void markNoticeSent(Instant noticeSentAt) {
        this.noticeSentAt = noticeSentAt;
        this.updatedAt = noticeSentAt;
        this.status = NoteRetentionStatus.NOTICE_SENT;
    }

    // markRemoved: Records successful automatic note removal.
    public void markRemoved(Instant removedAt) {
        this.note = null;
        this.removedAt = removedAt;
        this.updatedAt = removedAt;
        this.status = NoteRetentionStatus.REMOVED;
    }

    // cancel: Cancels automatic removal while keeping the audit snapshot.
    public void cancel(String reason, Instant cancelledAt) {
        this.cancelReason = reason;
        this.cancelledAt = cancelledAt;
        this.updatedAt = cancelledAt;
        this.status = NoteRetentionStatus.CANCELLED;
    }
}
