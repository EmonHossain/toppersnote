package com.sharenote.note.dto;

import java.time.Instant;

public record NoteUploadResponse(
        Long id,
        String subjectClass,
        String semester,
        String year,
        String originalFileName,
        String contentType,
        long fileSize,
        Long uploadedByUserId,
        Instant createdAt
) {
}
