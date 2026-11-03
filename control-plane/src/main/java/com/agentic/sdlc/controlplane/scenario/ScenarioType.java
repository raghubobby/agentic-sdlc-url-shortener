package com.agentic.sdlc.controlplane.scenario;

public enum ScenarioType {
    GREENFIELD,
    BROWNFIELD,
    AMBIGUOUS;

    public static ScenarioType from(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("scenario is required");
        return ScenarioType.valueOf(value.trim().toUpperCase().replace('-', '_'));
    }
}
