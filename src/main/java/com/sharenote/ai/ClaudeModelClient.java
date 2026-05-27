package com.sharenote.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class ClaudeModelClient extends AbstractHttpAiModelClient {

    public ClaudeModelClient(AiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    @Override
    public boolean supports(AiProvider provider) {
        return provider == AiProvider.ANTHROPIC_CLAUDE;
    }

    @Override
    public String complete(AiModelInvocation invocation) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", invocation.model());
        body.put("max_tokens", 2048);
        ArrayNode messages = body.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        if (invocation.attachNote() && invocation.attachedNoteFile() != null) {
            ObjectNode document = content.addObject();
            document.put("type", "document");
            ObjectNode source = document.putObject("source");
            source.put("type", "base64");
            source.put("media_type", invocation.attachedNoteFile().contentType());
            source.put("data", base64(invocation.attachedNoteFile()));
        }
        content.addObject().put("type", "text").put("text", invocation.prompt());

        String response = postJson(
                endpoint(properties.getAnthropicBaseUrl(), "/v1/messages"),
                body,
                invocation.apiKey(),
                (builder, apiKey) -> builder
                        .header("x-api-key", apiKey)
                        .header("anthropic-version", "2023-06-01")
        );
        return parseAnswer(response);
    }

    private String parseAnswer(String response) {
        try {
            JsonNode content = objectMapper.readTree(response).get("content");
            if (content != null && content.isArray()) {
                for (JsonNode block : content) {
                    String text = firstText(block.get("text"), null);
                    if (text != null) {
                        return text;
                    }
                }
            }
            return response;
        } catch (IOException exception) {
            throw new AiProviderException("Claude response could not be parsed", exception);
        }
    }
}
