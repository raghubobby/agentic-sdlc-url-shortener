package com.agentic.sdlc.urlshortener.api;

import java.time.Instant;

public record ClickEventResponse(
        Instant occurredAt,
        String referrerHost,
        String userAgentFamily
) {}
