package com.agentic.sdlc.urlshortener.api;

import java.time.Instant;

public record ShortUrlResponse(
        String code,
        String shortUrl,
        String destinationUrl,
        Instant createdAt,
        Instant expiresAt,
        boolean active,
        LinkStatus status
) {}
