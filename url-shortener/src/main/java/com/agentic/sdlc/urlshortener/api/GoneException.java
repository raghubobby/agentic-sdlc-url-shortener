package com.agentic.sdlc.urlshortener.api;

public class GoneException extends ApiException {
    public GoneException(String code) {
        super("SHORT_URL_INACTIVE", "Short URL '" + code + "' is expired or deactivated");
    }
}
