package com.sharenote.note.dto;

import java.time.Instant;

public record NoteResponse(
        Long id,
        String subjectClass,
        String semester,
        String year,
        String originalFileName,
        String contentType,
        long fileSize,
        Long uploadedByUserId,
        String uploadedByName,
        Instant createdAt
) {
}
