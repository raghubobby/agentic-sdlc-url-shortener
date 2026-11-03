package com.agentic.sdlc.urlshortener.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreateShortUrlRequest(
        @NotBlank @Size(max = 4096) String url,
        @Size(max = 32) String customAlias,
        Instant expiresAt
) {}
