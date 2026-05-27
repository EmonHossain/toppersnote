package com.sharenote.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class GrokModelClient extends AbstractHttpAiModelClient {

    public GrokModelClient(AiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    @Override
    public boolean supports(AiProvider provider) {
        return provider == AiProvider.XAI_GROK;
    }

    @Override
    public String complete(AiModelInvocation invocation) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", invocation.model());
        ArrayNode messages = body.putArray("messages");
        ObjectNode message = messages.addObject();
        message.put("role", "user");
        message.put("content", invocation.prompt());

        String response = postJson(
                endpoint(properties.getXaiBaseUrl(), "/v1/chat/completions"),
                body,
                invocation.apiKey(),
                (builder, apiKey) -> builder.header("Authorization", "Bearer " + apiKey)
        );
        return parseAnswer(response);
    }

    private String parseAnswer(String response) {
        try {
            JsonNode choices = objectMapper.readTree(response).get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                String text = firstText(choices.get(0).path("message").get("content"), null);
                if (text != null) {
                    return text;
                }
            }
            return response;
        } catch (IOException exception) {
            throw new AiProviderException("Grok response could not be parsed", exception);
        }
    }
}
