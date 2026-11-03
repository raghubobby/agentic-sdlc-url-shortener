package com.agentic.sdlc.urlshortener.api;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record AnalyticsResponse(
        String code,
        long totalClicks,
        Instant lastClickedAt,
        List<DailyClicks> lastSevenDays,
        List<DimensionCount> topReferrers,
        List<DimensionCount> browserFamilies
) {
    public record DailyClicks(LocalDate day, long clicks) {}
    public record DimensionCount(String value, long clicks) {}
}
