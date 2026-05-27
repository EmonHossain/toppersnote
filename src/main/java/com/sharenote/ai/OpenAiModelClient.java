package com.sharenote.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OpenAiModelClient extends AbstractHttpAiModelClient {

    public OpenAiModelClient(AiProperties properties, ObjectMapper objectMapper) {
        super(properties, objectMapper);
    }

    @Override
    public boolean supports(AiProvider provider) {
        return provider == AiProvider.OPENAI;
    }

    @Override
    public String complete(AiModelInvocation invocation) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", invocation.model());
        ArrayNode input = body.putArray("input");
        ObjectNode message = input.addObject();
        message.put("role", "user");
        ArrayNode content = message.putArray("content");
        content.addObject().put("type", "input_text").put("text", invocation.prompt());
        if (invocation.attachNote() && invocation.attachedNoteFile() != null) {
            content.addObject()
                    .put("type", "input_file")
                    .put("filename", invocation.attachedNoteFile().fileName())
                    .put("file_data", dataUrl(invocation.attachedNoteFile()));
        }

        String response = postJson(
                endpoint(properties.getOpenaiBaseUrl(), "/v1/responses"),
                body,
                invocation.apiKey(),
                (builder, apiKey) -> builder.header("Authorization", "Bearer " + apiKey)
        );
        return parseAnswer(response);
    }

    private String parseAnswer(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            String outputText = firstText(root.get("output_text"), null);
            if (outputText != null) {
                return outputText;
            }
            JsonNode output = root.get("output");
            if (output != null && output.isArray()) {
                for (JsonNode item : output) {
                    JsonNode content = item.get("content");
                    if (content != null && content.isArray()) {
                        for (JsonNode block : content) {
                            String text = firstText(block.get("text"), null);
                            if (text != null) {
                                return text;
                            }
                        }
                    }
                }
            }
            return response;
        } catch (IOException exception) {
            throw new AiProviderException("OpenAI response could not be parsed", exception);
        }
    }
}
