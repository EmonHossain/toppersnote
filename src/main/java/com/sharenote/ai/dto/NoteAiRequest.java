package com.sharenote.ai.dto;

import com.sharenote.ai.AiProvider;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NoteAiRequest(
        @NotNull(message = "AI provider is required")
        AiProvider provider,

        @NotBlank(message = "Model is required")
        @Size(max = 120, message = "Model must be at most 120 characters")
        String model,

        @NotBlank(message = "Prompt is required")
        @Size(max = 4000, message = "Prompt must be at most 4000 characters")
        String prompt,

        boolean attachNote,

        @NotBlank(message = "API key is required")
        @Size(max = 4000, message = "API key must be at most 4000 characters")
        String apiKey
) {
}
