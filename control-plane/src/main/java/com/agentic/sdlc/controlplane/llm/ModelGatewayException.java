package com.agentic.sdlc.controlplane.llm;

import com.agentic.sdlc.controlplane.recovery.FailureCategory;

public class ModelGatewayException extends RuntimeException {
    private final FailureCategory failureCategory;

    public ModelGatewayException(FailureCategory failureCategory, String message) {
        super(message);
        this.failureCategory = failureCategory;
    }

    public ModelGatewayException(FailureCategory failureCategory, String message, Throwable cause) {
        super(message, cause);
        this.failureCategory = failureCategory;
    }

    public FailureCategory failureCategory() {
        return failureCategory;
    }
}
