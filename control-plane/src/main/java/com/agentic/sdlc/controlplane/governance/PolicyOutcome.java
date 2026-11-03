package com.agentic.sdlc.controlplane.governance;

public record PolicyOutcome(
        PolicyDecision decision,
        RiskAssessment risk,
        int requiredApprovals,
        String reason
) {}
