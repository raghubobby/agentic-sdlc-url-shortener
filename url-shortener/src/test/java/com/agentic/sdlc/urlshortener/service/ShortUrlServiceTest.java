package com.agentic.sdlc.urlshortener.service;

import com.agentic.sdlc.urlshortener.api.CreateShortUrlRequest;
import com.agentic.sdlc.urlshortener.config.UrlShortenerProperties;
import com.agentic.sdlc.urlshortener.domain.ShortUrlEntity;
import com.agentic.sdlc.urlshortener.repository.ShortUrlRepository;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShortUrlServiceTest {
    @Test
    void retriesGeneratedCodeCollisionWithoutMakingCollisionAClientFailure() {
        ShortUrlRepository repository = mock(ShortUrlRepository.class);
        ShortCodeGenerator generator = mock(ShortCodeGenerator.class);
        RedirectCache cache = mock(RedirectCache.class);
        TargetUrlValidator validator = new TargetUrlValidator();
        RequestHasher hasher = new RequestHasher();
        UrlShortenerProperties properties = new UrlShortenerProperties("https://sho.rt/r", 7, Duration.ofMinutes(1), true);
        Clock clock = Clock.fixed(Instant.parse("2026-10-13T12:00:00Z"), ZoneOffset.UTC);

        when(generator.generate(7)).thenReturn("COLLID1", "GOOD123");
        when(repository.saveAndFlush(any(ShortUrlEntity.class)))
                .thenThrow(new DataIntegrityViolationException("short_code unique"))
                .thenAnswer(inv -> inv.getArgument(0));

        ShortUrlService service = new ShortUrlService(repository, generator, validator, hasher, cache, properties, clock);
        var result = service.create(new CreateShortUrlRequest("https://example.com/docs", null, null), null);

        assertThat(result.code()).isEqualTo("GOOD123");
        assertThat(result.shortUrl()).isEqualTo("https://sho.rt/r/GOOD123");
        verify(cache).put(any(), any(), any(), any());
    }
}
