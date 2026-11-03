package com.agentic.sdlc.controlplane.recovery;

public enum FailureCategory {
    TRANSIENT_INFRASTRUCTURE,
    RATE_LIMIT,
    VALIDATION,
    POLICY,
    EXECUTION_BUG,
    UNKNOWN
}
