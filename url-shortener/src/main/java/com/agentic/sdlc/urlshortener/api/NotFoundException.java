package com.agentic.sdlc.urlshortener.api;

public class NotFoundException extends ApiException {
    public NotFoundException(String code) {
        super("SHORT_URL_NOT_FOUND", "Short URL '" + code + "' was not found");
    }
}
