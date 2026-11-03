package com.agentic.sdlc.urlshortener.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ShortUrlEntityTest {
    @Test
    void expirationBoundaryControlsActiveState() {
        Instant expiresAt = Instant.parse("2026-10-14T12:00:00Z");
        ShortUrlEntity entity = new ShortUrlEntity(
                UUID.randomUUID(),
                "ABC1234",
                "https://example.com",
                Instant.parse("2026-10-14T11:00:00Z"),
                expiresAt,
                null,
                null
        );

        assertThat(entity.isActive(Instant.parse("2026-10-14T11:59:59Z"))).isTrue();
        assertThat(entity.isExpired(expiresAt)).isTrue();
        assertThat(entity.isActive(expiresAt)).isFalse();
    }
}
