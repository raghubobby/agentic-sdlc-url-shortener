package com.agentic.sdlc.controlplane.llm;

import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thin HTTP adapter for OpenAI's Responses API. Domain code depends only on ModelGateway;
 * this adapter can be replaced without changing agent or workflow semantics.
 */
@Component
public class OpenAiResponsesModelGateway {
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;
    private final String path;

    public OpenAiResponsesModelGateway(
            ObjectMapper objectMapper,
            @Value("${agentic.model.openai.api-key:}") String apiKey,
            @Value("${agentic.model.openai.model:}") String model,
            @Value("${agentic.model.openai.base-url:https://api.openai.com}") String baseUrl,
            @Value("${agentic.model.openai.path:/v1/responses}") String path) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
        this.baseUrl = baseUrl;
        this.path = path;
    }

    public ModelResponse generate(ModelRequest request) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ModelGatewayException(FailureCategory.VALIDATION,
                    "agentic.model.openai.api-key is required when provider=openai-responses");
        }
        if (model == null || model.isBlank()) {
            throw new ModelGatewayException(FailureCategory.VALIDATION,
                    "agentic.model.openai.model is required when provider=openai-responses");
        }

        long started = System.nanoTime();
        try {
            Map<String, Object> format = new LinkedHashMap<>();
            format.put("type", "json_schema");
            format.put("name", request.schemaName());
            format.put("strict", true);
            format.put("schema", request.responseSchema());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", model);
            body.put("input", request.prompt().render());
            body.put("text", Map.of("format", format));

            RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl);
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
            JsonNode root = builder.build().post().uri(path).body(body).retrieve().body(JsonNode.class);
            if (root == null) {
                throw new ModelGatewayException(FailureCategory.TRANSIENT_INFRASTRUCTURE,
                        "Model provider returned an empty response");
            }

            String refusal = findFirstText(root, "refusal");
            String outputText = findOutputText(root);
            if (refusal != null && !refusal.isBlank()) {
                return new ModelResponse(request.requestId(), "openai-responses", text(root, "model", model),
                        null, true, refusal, elapsedMillis(started), usage(root, "input_tokens"), usage(root, "output_tokens"));
            }
            if (outputText == null || outputText.isBlank()) {
                throw new ModelGatewayException(FailureCategory.VALIDATION,
                        "Model provider response did not contain structured output text");
            }

            JsonNode structured = objectMapper.readTree(outputText);
            return new ModelResponse(request.requestId(), "openai-responses", text(root, "model", model),
                    structured, false, null, elapsedMillis(started), usage(root, "input_tokens"), usage(root, "output_tokens"));
        } catch (RestClientResponseException ex) {
            FailureCategory category = ex.getStatusCode().value() == 429
                    ? FailureCategory.RATE_LIMIT
                    : ex.getStatusCode().is5xxServerError()
                    ? FailureCategory.TRANSIENT_INFRASTRUCTURE
                    : FailureCategory.VALIDATION;
            throw new ModelGatewayException(category,
                    "Model provider HTTP failure: " + ex.getStatusCode().value(), ex);
        } catch (ModelGatewayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ModelGatewayException(FailureCategory.UNKNOWN,
                    "Model provider invocation failed: " + ex.getMessage(), ex);
        }
    }

    private String findOutputText(JsonNode root) {
        if (root == null) return null;
        if (root.isObject()) {
            JsonNode directOutputText = root.get("output_text");
            if (directOutputText != null && directOutputText.isString() && !directOutputText.asText().isBlank()) {
                return directOutputText.asText();
            }
            JsonNode type = root.get("type");
            JsonNode text = root.get("text");
            if (type != null && "output_text".equals(type.asText()) && text != null && text.isString()) {
                return text.asText();
            }
            for (var entry : root.properties()) {
                String found = findOutputText(entry.getValue());
                if (found != null) return found;
            }
        } else if (root.isArray()) {
            for (JsonNode child : root) {
                String found = findOutputText(child);
                if (found != null) return found;
            }
        }
        return null;
    }

    private String findFirstText(JsonNode root, String fieldName) {
        if (root == null) return null;
        if (root.isObject()) {
            JsonNode direct = root.get(fieldName);
            if (direct != null && direct.isString() && !direct.asText().isBlank()) return direct.asText();
            for (var entry : root.properties()) {
                String found = findFirstText(entry.getValue(), fieldName);
                if (found != null) return found;
            }
        } else if (root.isArray()) {
            for (JsonNode child : root) {
                String found = findFirstText(child, fieldName);
                if (found != null) return found;
            }
        }
        return null;
    }

    private Integer usage(JsonNode root, String field) {
        JsonNode usage = root.path("usage");
        JsonNode value = usage.path(field);
        return value.isIntegralNumber() ? value.asInt() : null;
    }

    private String text(JsonNode root, String field, String fallback) {
        JsonNode value = root.get(field);
        return value != null && value.isString() ? value.asText() : fallback;
    }

    private long elapsedMillis(long started) {
        return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }
}
