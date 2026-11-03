package com.agentic.sdlc.urlshortener.service;

import com.agentic.sdlc.urlshortener.api.GoneException;
import com.agentic.sdlc.urlshortener.api.NotFoundException;
import com.agentic.sdlc.urlshortener.domain.ShortUrlEntity;
import com.agentic.sdlc.urlshortener.repository.ShortUrlRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
public class RedirectService {
    private final ShortUrlRepository repository;
    private final RedirectCache cache;
    private final AnalyticsService analytics;
    private final Clock clock = Clock.systemUTC();
    private final Counter cacheHits;
    private final Counter cacheMisses;
    private final Counter analyticsFailures;

    public RedirectService(ShortUrlRepository repository, RedirectCache cache, AnalyticsService analytics,
                           MeterRegistry meterRegistry) {
        this.repository = repository;
        this.cache = cache;
        this.analytics = analytics;
        this.cacheHits = Counter.builder("url.redirect.cache.hit").register(meterRegistry);
        this.cacheMisses = Counter.builder("url.redirect.cache.miss").register(meterRegistry);
        this.analyticsFailures = Counter.builder("url.analytics.record.failure").register(meterRegistry);
    }

    @Transactional(readOnly = true)
    public RedirectResult resolve(String code, String referrer, String userAgent) {
        var cached = cache.get(code);
        if (cached.isPresent()) {
            cacheHits.increment();
            if (cached.get().expiresAt() != null && !cached.get().expiresAt().isAfter(clock.instant())) {
                cache.evict(code);
                throw new GoneException(code);
            }
            recordAnalytics(cached.get().shortUrlId(), referrer, userAgent);
            return new RedirectResult(cached.get().shortUrlId(), cached.get().destination(), true);
        }

        cacheMisses.increment();
        ShortUrlEntity entity = repository.findByShortCode(code).orElseThrow(() -> new NotFoundException(code));
        ensureActive(entity, clock.instant());
        cache.put(code, entity.getId(), entity.getDestinationUrl(), entity.getExpiresAt());
        recordAnalytics(entity.getId(), referrer, userAgent);
        return new RedirectResult(entity.getId(), entity.getDestinationUrl(), false);
    }

    private void recordAnalytics(java.util.UUID shortUrlId, String referrer, String userAgent) {
        try {
            analytics.record(shortUrlId, referrer, userAgent);
        } catch (DataAccessException ex) {
            // Redirect availability is the primary SLO. Analytics persistence is isolated and observable.
            analyticsFailures.increment();
        }
    }

    private void ensureActive(ShortUrlEntity entity, Instant now) {
        if (!entity.isActive(now)) {
            cache.evict(entity.getShortCode());
            throw new GoneException(entity.getShortCode());
        }
    }
}
