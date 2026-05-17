package com.sharenote.note.dto;

import java.time.Instant;

public record NoteResponse(
        Long id,
        String subjectClass,
        String institution,
        String degreeProgram,
        String semester,
        String year,
        String originalFileName,
        String contentType,
        long fileSize,
        Long uploadedByUserId,
        String uploadedByName,
        long commentCount,
        long upvoteCount,
        long takeALookSuggestionCount,
        Instant createdAt
) {
}
