package com.agentic.sdlc.urlshortener.service;

import com.agentic.sdlc.urlshortener.api.AnalyticsResponse;
import com.agentic.sdlc.urlshortener.api.ClickEventResponse;
import com.agentic.sdlc.urlshortener.api.DetailedAnalyticsResponse;
import com.agentic.sdlc.urlshortener.api.NotFoundException;
import com.agentic.sdlc.urlshortener.domain.ClickEventEntity;
import com.agentic.sdlc.urlshortener.repository.ClickEventRepository;
import com.agentic.sdlc.urlshortener.repository.ShortUrlRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsService {
    private static final int MAX_DETAIL_EVENTS = 100;
    private static final int MAX_DIMENSIONS = 5;

    private final ClickEventRepository clickRepository;
    private final ShortUrlRepository shortUrlRepository;
    private final Clock clock = Clock.systemUTC();

    public AnalyticsService(ClickEventRepository clickRepository, ShortUrlRepository shortUrlRepository) {
        this.clickRepository = clickRepository;
        this.shortUrlRepository = shortUrlRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID shortUrlId, String referrer, String userAgent) {
        clickRepository.save(new ClickEventEntity(UUID.randomUUID(), shortUrlId, clock.instant(),
                referrerHost(referrer), userAgentFamily(userAgent)));
    }

    @Transactional(readOnly = true)
    public AnalyticsResponse analytics(String code) {
        var shortUrl = shortUrlRepository.findByShortCode(code).orElseThrow(() -> new NotFoundException(code));
        UUID shortUrlId = shortUrl.getId();
        long total = clickRepository.countByShortUrlId(shortUrlId);
        Instant lastClickedAt = clickRepository.findTopByShortUrlIdOrderByOccurredAtDesc(shortUrlId)
                .map(ClickEventEntity::getOccurredAt).orElse(null);

        LocalDate firstDay = LocalDate.now(clock).minusDays(6);
        Instant since = firstDay.atStartOfDay(ZoneOffset.UTC).toInstant();
        Map<LocalDate, Long> countsByDay = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) countsByDay.put(firstDay.plusDays(i), 0L);
        for (ClickEventEntity event : clickRepository
                .findByShortUrlIdAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(shortUrlId, since)) {
            LocalDate day = event.getOccurredAt().atZone(ZoneOffset.UTC).toLocalDate();
            if (countsByDay.containsKey(day)) countsByDay.computeIfPresent(day, (ignored, count) -> count + 1);
        }

        List<AnalyticsResponse.DailyClicks> daily = countsByDay.entrySet().stream()
                .map(e -> new AnalyticsResponse.DailyClicks(e.getKey(), e.getValue()))
                .toList();

        return new AnalyticsResponse(
                code,
                total,
                lastClickedAt,
                daily,
                dimensions(clickRepository.referrerCounts(shortUrlId), "Direct / unknown"),
                dimensions(clickRepository.browserCounts(shortUrlId), "Unknown")
        );
    }

    @Transactional(readOnly = true)
    public DetailedAnalyticsResponse events(String code, int requestedLimit) {
        var shortUrl = shortUrlRepository.findByShortCode(code).orElseThrow(() -> new NotFoundException(code));
        int limit = Math.max(1, Math.min(MAX_DETAIL_EVENTS, requestedLimit));
        List<ClickEventResponse> events = clickRepository
                .findByShortUrlIdOrderByOccurredAtDesc(shortUrl.getId(), PageRequest.of(0, limit))
                .stream()
                .map(event -> new ClickEventResponse(
                        event.getOccurredAt(),
                        event.getReferrerHost(),
                        event.getUserAgentFamily()))
                .toList();
        return new DetailedAnalyticsResponse(
                code,
                clickRepository.countByShortUrlId(shortUrl.getId()),
                events);
    }

    private List<AnalyticsResponse.DimensionCount> dimensions(List<Object[]> rows, String emptyLabel) {
        List<AnalyticsResponse.DimensionCount> result = new ArrayList<>();
        for (Object[] row : rows) {
            if (result.size() >= MAX_DIMENSIONS) break;
            String value = row[0] == null || row[0].toString().isBlank() ? emptyLabel : row[0].toString();
            result.add(new AnalyticsResponse.DimensionCount(value, ((Number) row[1]).longValue()));
        }
        return result;
    }

    private String referrerHost(String referrer) {
        if (referrer == null || referrer.isBlank()) return null;
        try {
            String host = URI.create(referrer).getHost();
            return host == null ? null : truncate(host.toLowerCase(Locale.ROOT), 255);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String userAgentFamily(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return null;
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("edg/")) return "Edge";
        if (ua.contains("chrome/")) return "Chrome";
        if (ua.contains("firefox/")) return "Firefox";
        if (ua.contains("safari/") && !ua.contains("chrome/")) return "Safari";
        if (ua.contains("curl/")) return "curl";
        return "Other";
    }

    private String truncate(String value, int max) {
        return value.length() <= max ? value : value.substring(0, max);
    }
}
