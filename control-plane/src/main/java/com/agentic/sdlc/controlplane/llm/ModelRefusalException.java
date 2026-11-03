package com.agentic.sdlc.controlplane.llm;

public class ModelRefusalException extends RuntimeException {
    public ModelRefusalException(String message) {
        super(message);
    }
}
