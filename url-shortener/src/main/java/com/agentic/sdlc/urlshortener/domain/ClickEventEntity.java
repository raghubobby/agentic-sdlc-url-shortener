package com.agentic.sdlc.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "click_event", schema = "url_shortener")
public class ClickEventEntity {
    @Id
    private UUID id;

    @Column(name = "short_url_id", nullable = false)
    private UUID shortUrlId;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Column(name = "referrer_host", length = 255)
    private String referrerHost;

    @Column(name = "user_agent_family", length = 100)
    private String userAgentFamily;

    protected ClickEventEntity() {}

    public ClickEventEntity(UUID id, UUID shortUrlId, Instant occurredAt, String referrerHost, String userAgentFamily) {
        this.id = id;
        this.shortUrlId = shortUrlId;
        this.occurredAt = occurredAt;
        this.referrerHost = referrerHost;
        this.userAgentFamily = userAgentFamily;
    }

    public UUID getId() { return id; }
    public UUID getShortUrlId() { return shortUrlId; }
    public Instant getOccurredAt() { return occurredAt; }
    public String getReferrerHost() { return referrerHost; }
    public String getUserAgentFamily() { return userAgentFamily; }
}
