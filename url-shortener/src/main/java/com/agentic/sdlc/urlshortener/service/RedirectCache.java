package com.agentic.sdlc.urlshortener.service;

import com.agentic.sdlc.urlshortener.config.UrlShortenerProperties;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@Component
public class RedirectCache {
    private static final String ID = "id";
    private static final String DESTINATION = "destination";
    private static final String EXPIRES_AT = "expiresAt";
    private final StringRedisTemplate redis;
    private final UrlShortenerProperties properties;
    private final Counter cacheErrors;

    public RedirectCache(StringRedisTemplate redis, UrlShortenerProperties properties, MeterRegistry meterRegistry) {
        this.redis = redis;
        this.properties = properties;
        this.cacheErrors = Counter.builder("url.redirect.cache.error").register(meterRegistry);
    }

    public Optional<CachedRedirect> get(String code) {
        if (!properties.cacheEnabled()) return Optional.empty();
        try {
            Map<Object, Object> values = redis.opsForHash().entries(key(code));
            if (values.isEmpty()) return Optional.empty();
            String id = (String) values.get(ID);
            String destination = (String) values.get(DESTINATION);
            String expiresAt = (String) values.get(EXPIRES_AT);
            if (id == null || destination == null) return Optional.empty();
            Instant expiry = expiresAt == null || expiresAt.isBlank() ? null : Instant.parse(expiresAt);
            if (expiry != null && !expiry.isAfter(Instant.now())) {
                evict(code);
                return Optional.empty();
            }
            return Optional.of(new CachedRedirect(java.util.UUID.fromString(id), destination, expiry));
        } catch (DataAccessException ex) {
            cacheErrors.increment();
            return Optional.empty(); // cache is an optimization; database remains authoritative
        }
    }

    public void put(String code, java.util.UUID id, String destination, Instant expiresAt) {
        if (!properties.cacheEnabled()) return;
        try {
            String key = key(code);
            redis.opsForHash().put(key, ID, id.toString());
            redis.opsForHash().put(key, DESTINATION, destination);
            redis.opsForHash().put(key, EXPIRES_AT, expiresAt == null ? "" : expiresAt.toString());
            Duration ttl = properties.cacheTtl();
            if (expiresAt != null) {
                Duration remaining = Duration.between(Instant.now(), expiresAt);
                if (remaining.isNegative() || remaining.isZero()) {
                    evict(code);
                    return;
                }
                if (remaining.compareTo(ttl) < 0) ttl = remaining;
            }
            redis.expire(key, ttl);
        } catch (DataAccessException ignored) {
            cacheErrors.increment();
            // fail open: redirect correctness does not depend on Redis availability
        }
    }

    public void evict(String code) {
        if (!properties.cacheEnabled()) return;
        try {
            redis.delete(key(code));
        } catch (DataAccessException ignored) {
            cacheErrors.increment();
            // best-effort cache invalidation; stale entries are also bounded by TTL
        }
    }

    private String key(String code) { return "url:redirect:" + code; }

    public record CachedRedirect(java.util.UUID shortUrlId, String destination, Instant expiresAt) {}
}
