package com.agentic.sdlc.controlplane.scenario;

import java.util.List;

public record ScenarioBlueprint(
        ScenarioType type,
        String title,
        String requirement,
        String engineeringIntent,
        List<String> validationCriteria,
        List<String> interviewSignals
) {
    public ScenarioBlueprint {
        validationCriteria = List.copyOf(validationCriteria);
        interviewSignals = List.copyOf(interviewSignals);
    }
}
