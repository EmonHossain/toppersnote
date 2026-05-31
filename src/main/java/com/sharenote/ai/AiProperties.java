package com.sharenote.ai;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private int maxPromptChars = 4000;
    private long maxAttachmentBytes = 10_485_760;
    private int requestTimeoutSeconds = 30;
    private String openaiBaseUrl = "https://api.openai.com";
    private String anthropicBaseUrl = "https://api.anthropic.com";
    private String googleGeminiBaseUrl = "https://generativelanguage.googleapis.com";
    private String xaiBaseUrl = "https://api.x.ai";

    public int getMaxPromptChars() {
        return maxPromptChars;
    }

    public void setMaxPromptChars(int maxPromptChars) {
        this.maxPromptChars = maxPromptChars;
    }

    public long getMaxAttachmentBytes() {
        return maxAttachmentBytes;
    }

    public void setMaxAttachmentBytes(long maxAttachmentBytes) {
        this.maxAttachmentBytes = maxAttachmentBytes;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public String getOpenaiBaseUrl() {
        return openaiBaseUrl;
    }

    public void setOpenaiBaseUrl(String openaiBaseUrl) {
        this.openaiBaseUrl = openaiBaseUrl;
    }

    public String getAnthropicBaseUrl() {
        return anthropicBaseUrl;
    }

    public void setAnthropicBaseUrl(String anthropicBaseUrl) {
        this.anthropicBaseUrl = anthropicBaseUrl;
    }

    public String getGoogleGeminiBaseUrl() {
        return googleGeminiBaseUrl;
    }

    public void setGoogleGeminiBaseUrl(String googleGeminiBaseUrl) {
        this.googleGeminiBaseUrl = googleGeminiBaseUrl;
    }

    public String getXaiBaseUrl() {
        return xaiBaseUrl;
    }

    public void setXaiBaseUrl(String xaiBaseUrl) {
        this.xaiBaseUrl = xaiBaseUrl;
    }
}
