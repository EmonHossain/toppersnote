package com.sharenote.ai;

public record AiModelInvocation(
        AiProvider provider,
        String model,
        String prompt,
        String apiKey,
        boolean attachNote,
        AttachedNoteFile attachedNoteFile
) {
}
