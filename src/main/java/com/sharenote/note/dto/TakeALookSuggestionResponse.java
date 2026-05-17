package com.sharenote.note.dto;

import java.time.Instant;

public record TakeALookSuggestionResponse(
        Long id,
        Long noteId,
        String subjectClass,
        Long suggestedByUserId,
        String suggestedByName,
        Long suggestedToUserId,
        String suggestedToName,
        String message,
        Instant createdAt
) {
}
