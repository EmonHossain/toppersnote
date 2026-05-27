package com.sharenote.ai;

import java.util.List;

public enum AiProvider {
    OPENAI("OpenAI", true, List.of("gpt-4.1", "gpt-4.1-mini")),
    ANTHROPIC_CLAUDE("Claude", true, List.of("claude-3-5-sonnet-latest", "claude-3-5-haiku-latest")),
    GOOGLE_GEMINI("Google Gemini", true, List.of("gemini-1.5-pro", "gemini-1.5-flash")),
    XAI_GROK("Grok", false, List.of("grok-2-latest"));

    private final String displayName;
    private final boolean fileAttachmentSupported;
    private final List<String> suggestedModels;

    AiProvider(String displayName, boolean fileAttachmentSupported, List<String> suggestedModels) {
        this.displayName = displayName;
        this.fileAttachmentSupported = fileAttachmentSupported;
        this.suggestedModels = suggestedModels;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isFileAttachmentSupported() {
        return fileAttachmentSupported;
    }

    public List<String> getSuggestedModels() {
        return suggestedModels;
    }
}
