package com.agentic.sdlc.urlshortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "url-shortener")
public record UrlShortenerProperties(
        String baseUrl,
        int codeLength,
        Duration cacheTtl,
        boolean cacheEnabled
) {
    public UrlShortenerProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("url-shortener.base-url is required");
        }
        if (codeLength < 6 || codeLength > 20) {
            throw new IllegalArgumentException("url-shortener.code-length must be between 6 and 20");
        }
        if (cacheTtl == null || cacheTtl.isNegative() || cacheTtl.isZero()) {
            throw new IllegalArgumentException("url-shortener.cache-ttl must be positive");
        }
    }
}
