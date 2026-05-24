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

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "notes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Note {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploaded_by_user_id", nullable = false)
    private User uploadedBy;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Note(
            String subjectClass,
            String institution,
            String degreeProgram,
            String semester,
            String year,
            String originalFileName,
            String storedFileName,
            String contentType,
            long fileSize,
            String storagePath,
            User uploadedBy,
            Instant createdAt
    ) {
        this.subjectClass = subjectClass;
        this.institution = institution;
        this.degreeProgram = degreeProgram;
        this.semester = semester;
        this.year = year;
        this.originalFileName = originalFileName;
        this.storedFileName = storedFileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.storagePath = storagePath;
        this.uploadedBy = uploadedBy;
        this.createdAt = createdAt;
    }

    public void setFileHash(String fileHash) {
        this.fileHash = fileHash;
    }
}
