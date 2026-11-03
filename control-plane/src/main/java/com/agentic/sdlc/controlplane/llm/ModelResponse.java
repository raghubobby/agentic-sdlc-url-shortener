package com.agentic.sdlc.controlplane.llm;

import tools.jackson.databind.JsonNode;

import java.util.UUID;

public record ModelResponse(
        UUID requestId,
        String provider,
        String model,
        JsonNode structuredOutput,
        boolean refused,
        String refusalReason,
        long latencyMs,
        Integer inputTokens,
        Integer outputTokens
) {}
