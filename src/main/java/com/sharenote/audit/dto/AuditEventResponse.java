package com.sharenote.audit.dto;

import java.time.Instant;

public record AuditEventResponse(
        Long id,
        String action,
        Long actorUserId,
        String actorEmail,
        String targetType,
        Long targetId,
        String message,
        String metadata,
        Instant createdAt
) {
}
