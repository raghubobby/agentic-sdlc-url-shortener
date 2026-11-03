package com.agentic.sdlc.urlshortener.service;

import java.util.UUID;

public record RedirectResult(UUID shortUrlId, String destinationUrl, boolean cacheHit) {}
