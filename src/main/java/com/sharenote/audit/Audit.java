package com.sharenote.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "audit_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private AuditAction action;

    private Long actorUserId;

    @Column(length = 320)
    private String actorEmail;

    @Column(nullable = false, length = 80)
    private String targetType;

    private Long targetId;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(length = 2000)
    private String metadata;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public Audit(
            AuditAction action,
            Long actorUserId,
            String actorEmail,
            String targetType,
            Long targetId,
            String message,
            String metadata,
            Instant createdAt
    ) {
        this.action = action;
        this.actorUserId = actorUserId;
        this.actorEmail = actorEmail;
        this.targetType = targetType;
        this.targetId = targetId;
        this.message = message;
        this.metadata = metadata;
        this.createdAt = createdAt;
    }
}

