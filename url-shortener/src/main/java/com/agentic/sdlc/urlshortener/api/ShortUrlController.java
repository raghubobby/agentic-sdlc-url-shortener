package com.agentic.sdlc.urlshortener.api;

import com.agentic.sdlc.urlshortener.service.AnalyticsService;
import com.agentic.sdlc.urlshortener.service.ShortUrlService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/urls")
public class ShortUrlController {
    private final ShortUrlService shortUrlService;
    private final AnalyticsService analyticsService;

    public ShortUrlController(ShortUrlService shortUrlService, AnalyticsService analyticsService) {
        this.shortUrlService = shortUrlService;
        this.analyticsService = analyticsService;
    }

    @PostMapping
    ResponseEntity<ShortUrlResponse> create(@Valid @RequestBody CreateShortUrlRequest request,
                                             @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        ShortUrlResponse response = shortUrlService.create(request, idempotencyKey);
        return ResponseEntity.created(URI.create(response.shortUrl())).body(response);
    }

    @GetMapping("/{code}")
    ShortUrlResponse get(@PathVariable String code) {
        return shortUrlService.get(code);
    }

    @GetMapping("/{code}/analytics")
    AnalyticsResponse analytics(@PathVariable String code) {
        return analyticsService.analytics(code);
    }

    @GetMapping("/{code}/analytics/events")
    DetailedAnalyticsResponse analyticsEvents(@PathVariable String code,
                                              @RequestParam(defaultValue = "25") int limit) {
        return analyticsService.events(code, limit);
    }

    @DeleteMapping("/{code}")
    ResponseEntity<Void> deactivate(@PathVariable String code) {
        shortUrlService.deactivate(code);
        return ResponseEntity.noContent().build();
    }
}
