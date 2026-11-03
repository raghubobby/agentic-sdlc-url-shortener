package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.tool.ToolAction;

import java.util.List;

public record AgentOutput(
        String summary,
        double confidence,
        List<String> assumptions,
        List<String> ambiguities,
        List<String> decisions,
        List<String> risks,
        List<String> requestedCapabilities,
        List<String> evidence,
        List<ToolAction> toolActions
) {
    public AgentOutput {
        if (summary == null || summary.isBlank()) throw new IllegalArgumentException("summary is required");
        if (confidence < 0.0 || confidence > 1.0) throw new IllegalArgumentException("confidence must be between 0 and 1");
        assumptions = List.copyOf(assumptions);
        ambiguities = List.copyOf(ambiguities);
        decisions = List.copyOf(decisions);
        risks = List.copyOf(risks);
        requestedCapabilities = List.copyOf(requestedCapabilities);
        evidence = List.copyOf(evidence);
        toolActions = List.copyOf(toolActions);
    }
}
