package com.agentic.sdlc.urlshortener.api;

public abstract class ApiException extends RuntimeException {
    private final String code;

    protected ApiException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
