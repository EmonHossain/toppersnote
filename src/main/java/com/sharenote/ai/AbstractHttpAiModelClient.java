package com.sharenote.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

abstract class AbstractHttpAiModelClient implements AiModelClient {

    protected final AiProperties properties;
    protected final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    AbstractHttpAiModelClient(AiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                .build();
    }

    protected String postJson(URI uri, ObjectNode body, String apiKey, HeaderConfigurer headerConfigurer) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));
            headerConfigurer.configure(builder, apiKey);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new AiProviderException("AI provider request failed with status " + response.statusCode());
            }
            return response.body();
        } catch (IOException exception) {
            throw new AiProviderException("AI provider response could not be processed", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AiProviderException("AI provider request was interrupted", exception);
        }
    }

    protected URI endpoint(String baseUrl, String path) {
        URI base = URI.create(baseUrl.trim());
        if (!"https".equalsIgnoreCase(base.getScheme())) {
            throw new AiProviderException("AI provider endpoint must use HTTPS");
        }
        return base.resolve(path);
    }

    protected String base64(AttachedNoteFile file) {
        return Base64.getEncoder().encodeToString(file.bytes());
    }

    protected String dataUrl(AttachedNoteFile file) {
        return "data:" + file.contentType() + ";base64," + base64(file);
    }

    protected String firstText(JsonNode node, String fallback) {
        if (node != null && node.isTextual() && !node.asText().isBlank()) {
            return node.asText();
        }
        return fallback;
    }

    @FunctionalInterface
    protected interface HeaderConfigurer {
        void configure(HttpRequest.Builder builder, String apiKey);
    }
}
