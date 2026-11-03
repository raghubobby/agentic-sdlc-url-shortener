package com.agentic.sdlc.controlplane.governance;

import java.util.List;

public record RiskAssessment(RiskLevel level, int score, List<String> reasons) {
    public RiskAssessment {
        reasons = List.copyOf(reasons);
    }
}
