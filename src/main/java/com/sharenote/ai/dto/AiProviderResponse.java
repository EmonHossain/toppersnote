package com.sharenote.ai.dto;

import com.sharenote.ai.AiProvider;

import java.util.List;

public record AiProviderResponse(
        AiProvider provider,
        String displayName,
        String authType,
        boolean fileAttachmentSupported,
        List<String> authFields,
        List<String> suggestedModels
) {
}
