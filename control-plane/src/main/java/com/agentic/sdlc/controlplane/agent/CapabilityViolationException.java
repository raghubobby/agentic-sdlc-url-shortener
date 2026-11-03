package com.agentic.sdlc.controlplane.agent;

import java.util.Set;

public class CapabilityViolationException extends RuntimeException {
    private final Set<String> violations;

    public CapabilityViolationException(Set<String> violations) {
        super("Agent requested capabilities outside its authority: " + violations);
        this.violations = Set.copyOf(violations);
    }

    public Set<String> violations() {
        return violations;
    }
}
