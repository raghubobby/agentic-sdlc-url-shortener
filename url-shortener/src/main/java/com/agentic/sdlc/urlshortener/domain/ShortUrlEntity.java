package com.agentic.sdlc.urlshortener.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "short_url", schema = "url_shortener")
public class ShortUrlEntity {
    @Id
    private UUID id;

    @Column(name = "short_code", nullable = false, unique = true, length = 32)
    private String shortCode;

    @Column(name = "destination_url", nullable = false, length = 4096)
    private String destinationUrl;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "deactivated_at")
    private Instant deactivatedAt;

    @Column(name = "idempotency_key", unique = true, length = 200)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

    @Version
    private long version;

    protected ShortUrlEntity() {}

    public ShortUrlEntity(UUID id, String shortCode, String destinationUrl, Instant createdAt,
                          Instant expiresAt, String idempotencyKey, String requestHash) {
        this.id = id;
        this.shortCode = shortCode;
        this.destinationUrl = destinationUrl;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
    }

    public UUID getId() { return id; }
    public String getShortCode() { return shortCode; }
    public String getDestinationUrl() { return destinationUrl; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getDeactivatedAt() { return deactivatedAt; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public long getVersion() { return version; }

    public boolean isExpired(Instant now) {
        return expiresAt != null && !expiresAt.isAfter(now);
    }

    public boolean isActive(Instant now) {
        return deactivatedAt == null && !isExpired(now);
    }

    public void deactivate(Instant now) {
        if (deactivatedAt == null) {
            deactivatedAt = now;
        }
    }
}
