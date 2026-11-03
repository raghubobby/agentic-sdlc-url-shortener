package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.recovery.FailureCategory;

public class ToolExecutionException extends RuntimeException {
    private final FailureCategory failureCategory;
    private final boolean sideEffectsPossible;

    public ToolExecutionException(FailureCategory failureCategory, String message, boolean sideEffectsPossible) {
        super(message);
        this.failureCategory = failureCategory;
        this.sideEffectsPossible = sideEffectsPossible;
    }

    public FailureCategory failureCategory() { return failureCategory; }
    public boolean sideEffectsPossible() { return sideEffectsPossible; }
}
