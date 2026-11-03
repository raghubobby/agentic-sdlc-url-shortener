package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.tool.ToolActionType;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public final class AgentOutputSchema {
    private AgentOutputSchema() {}

    public static Map<String, Object> jsonSchema() {
        Map<String, Object> stringArray = Map.of(
                "type", "array",
                "items", Map.of("type", "string"));
        List<String> capabilityNames = Arrays.stream(AgentCapability.values()).map(Enum::name).toList();
        List<String> toolNames = Arrays.stream(ToolActionType.values()).map(Enum::name).toList();
        Map<String, Object> capabilityArray = Map.of(
                "type", "array",
                "items", Map.of("type", "string", "enum", capabilityNames));
        Map<String, Object> nullableString = Map.of("type", List.of("string", "null"));
        Map<String, Object> toolAction = Map.of(
                "type", "object",
                "properties", Map.of(
                        "tool", Map.of("type", "string", "enum", toolNames),
                        "path", nullableString,
                        "content", nullableString,
                        "module", nullableString),
                "required", List.of("tool", "path", "content", "module"),
                "additionalProperties", false);
        Map<String, Object> toolActions = Map.of(
                "type", "array",
                "items", toolAction);

        return Map.of(
                "type", "object",
                "properties", Map.of(
                        "summary", Map.of("type", "string"),
                        "confidence", Map.of("type", "number", "minimum", 0, "maximum", 1),
                        "assumptions", stringArray,
                        "ambiguities", stringArray,
                        "decisions", stringArray,
                        "risks", stringArray,
                        "requestedCapabilities", capabilityArray,
                        "evidence", stringArray,
                        "toolActions", toolActions),
                "required", List.of("summary", "confidence", "assumptions", "ambiguities", "decisions",
                        "risks", "requestedCapabilities", "evidence", "toolActions"),
                "additionalProperties", false);
    }
}
