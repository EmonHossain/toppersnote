package com.sharenote.ai.dto;

import com.sharenote.ai.AiProvider;

import java.time.Instant;

public record NoteAiResponse(
        Long noteId,
        AiProvider provider,
        String model,
        String answer,
        Instant createdAt
) {
}
