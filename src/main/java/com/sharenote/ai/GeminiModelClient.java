package com.sharenote.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class GeminiModelClient extends AbstractHttpAiModelClient {

    public GeminiModelClient(AiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    @Override
    public boolean supports(AiProvider provider) {
        return provider == AiProvider.GOOGLE_GEMINI;
    }

    @Override
    public String complete(AiModelInvocation invocation) {
        ObjectNode body = objectMapper.createObjectNode();
        ArrayNode contents = body.putArray("contents");
        ObjectNode userContent = contents.addObject();
        ArrayNode parts = userContent.putArray("parts");
        parts.addObject().put("text", invocation.prompt());
        if (invocation.attachNote() && invocation.attachedNoteFile() != null) {
            ObjectNode inlineData = parts.addObject().putObject("inline_data");
            inlineData.put("mime_type", invocation.attachedNoteFile().contentType());
            inlineData.put("data", base64(invocation.attachedNoteFile()));
        }

        String encodedModel = URLEncoder.encode(invocation.model(), StandardCharsets.UTF_8);
        String encodedKey = URLEncoder.encode(invocation.apiKey(), StandardCharsets.UTF_8);
        String response = postJson(
                endpoint(properties.getGoogleGeminiBaseUrl(), "/v1beta/models/" + encodedModel + ":generateContent?key=" + encodedKey),
                body,
                invocation.apiKey(),
                (builder, apiKey) -> {
                }
        );
        return parseAnswer(response);
    }

    private String parseAnswer(String response) {
        try {
            JsonNode candidates = objectMapper.readTree(response).get("candidates");
            if (candidates != null && candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray()) {
                    StringBuilder answer = new StringBuilder();
                    for (JsonNode part : parts) {
                        String text = firstText(part.get("text"), null);
                        if (text != null) {
                            answer.append(text);
                        }
                    }
                    if (!answer.isEmpty()) {
                        return answer.toString();
                    }
                }
            }
            return response;
        } catch (IOException exception) {
            throw new AiProviderException("Gemini response could not be parsed", exception);
        }
    }
}
