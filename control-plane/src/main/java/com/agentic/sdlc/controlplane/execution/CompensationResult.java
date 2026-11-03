package com.agentic.sdlc.controlplane.execution;

public record CompensationResult(boolean success, String message) {
    public static CompensationResult succeeded(String message) {
        return new CompensationResult(true, message);
    }

    public static CompensationResult failed(String message) {
        return new CompensationResult(false, message);
    }
}
