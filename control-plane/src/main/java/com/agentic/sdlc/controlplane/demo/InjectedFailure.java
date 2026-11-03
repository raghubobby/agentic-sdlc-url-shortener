package com.agentic.sdlc.controlplane.demo;

import com.agentic.sdlc.controlplane.recovery.FailureCategory;

public record InjectedFailure(
        FailureCategory category,
        String message,
        boolean sideEffectsPossible
) {}
