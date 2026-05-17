package com.sharenote.notification.dto;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String type,
        Long noteId,
        String noteSubjectClass,
        Long actorUserId,
        String actorName,
        String title,
        String message,
        boolean read,
        Instant readAt,
        Instant createdAt
) {
}
