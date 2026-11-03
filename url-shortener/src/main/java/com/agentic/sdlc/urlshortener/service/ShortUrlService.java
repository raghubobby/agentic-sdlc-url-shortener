package com.agentic.sdlc.urlshortener.service;

import com.agentic.sdlc.urlshortener.api.ConflictException;
import com.agentic.sdlc.urlshortener.api.CreateShortUrlRequest;
import com.agentic.sdlc.urlshortener.api.NotFoundException;
import com.agentic.sdlc.urlshortener.api.LinkStatus;
import com.agentic.sdlc.urlshortener.api.ShortUrlResponse;
import com.agentic.sdlc.urlshortener.config.UrlShortenerProperties;
import com.agentic.sdlc.urlshortener.domain.ShortUrlEntity;
import com.agentic.sdlc.urlshortener.repository.ShortUrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class ShortUrlService {
    private static final Pattern ALIAS = Pattern.compile("^[A-Za-z0-9_-]{4,32}$");
    private static final int CODE_GENERATION_ATTEMPTS = 8;

    private final ShortUrlRepository repository;
    private final ShortCodeGenerator codeGenerator;
    private final TargetUrlValidator targetUrlValidator;
    private final RequestHasher requestHasher;
    private final RedirectCache redirectCache;
    private final UrlShortenerProperties properties;
    private final Clock clock;

    @Autowired
    public ShortUrlService(ShortUrlRepository repository,
                           ShortCodeGenerator codeGenerator,
                           TargetUrlValidator targetUrlValidator,
                           RequestHasher requestHasher,
                           RedirectCache redirectCache,
                           UrlShortenerProperties properties) {
        this(repository, codeGenerator, targetUrlValidator, requestHasher, redirectCache, properties, Clock.systemUTC());
    }

    ShortUrlService(ShortUrlRepository repository, ShortCodeGenerator codeGenerator,
                    TargetUrlValidator targetUrlValidator, RequestHasher requestHasher,
                    RedirectCache redirectCache, UrlShortenerProperties properties, Clock clock) {
        this.repository = repository;
        this.codeGenerator = codeGenerator;
        this.targetUrlValidator = targetUrlValidator;
        this.requestHasher = requestHasher;
        this.redirectCache = redirectCache;
        this.properties = properties;
        this.clock = clock;
    }

    public ShortUrlResponse create(CreateShortUrlRequest request, String idempotencyKey) {
        String normalizedUrl = targetUrlValidator.validateAndNormalize(request.url());
        Instant now = clock.instant();
        if (request.expiresAt() != null && !request.expiresAt().isAfter(now)) {
            throw new com.agentic.sdlc.urlshortener.api.InvalidRequestException("expiresAt", "must be in the future");
        }
        String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
        String requestHash = requestHasher.hash(new CreateShortUrlRequest(normalizedUrl, request.customAlias(), request.expiresAt()));

        if (normalizedKey != null) {
            var existing = repository.findByIdempotencyKey(normalizedKey);
            if (existing.isPresent()) return validateIdempotentReplay(existing.get(), requestHash);
        }

        boolean customAlias = request.customAlias() != null && !request.customAlias().isBlank();
        String requestedAlias = customAlias ? validateAliasSyntax(request.customAlias().trim()) : null;

        for (int attempt = 1; attempt <= CODE_GENERATION_ATTEMPTS; attempt++) {
            String code = customAlias ? requestedAlias : codeGenerator.generate(properties.codeLength());
            ShortUrlEntity entity = new ShortUrlEntity(UUID.randomUUID(), code, normalizedUrl, now,
                    request.expiresAt(), normalizedKey, requestHash);
            try {
                entity = repository.saveAndFlush(entity);
                redirectCache.put(entity.getShortCode(), entity.getId(), entity.getDestinationUrl(), entity.getExpiresAt());
                return toResponse(entity);
            } catch (DataIntegrityViolationException ex) {
                if (normalizedKey != null) {
                    var existing = repository.findByIdempotencyKey(normalizedKey);
                    if (existing.isPresent()) return validateIdempotentReplay(existing.get(), requestHash);
                }
                if (customAlias) {
                    throw new ConflictException("CUSTOM_ALIAS_TAKEN", "Custom alias is already in use");
                }
                // A generated-code uniqueness race is safe to retry with a new cryptographically random code.
            }
        }
        throw new ConflictException("SHORT_CODE_EXHAUSTED", "Unable to allocate a unique short code");
    }

    @Transactional(readOnly = true)
    public ShortUrlResponse get(String code) {
        return toResponse(repository.findByShortCode(code).orElseThrow(() -> new NotFoundException(code)));
    }

    @Transactional
    public void deactivate(String code) {
        ShortUrlEntity entity = repository.findByShortCode(code).orElseThrow(() -> new NotFoundException(code));
        entity.deactivate(clock.instant());
        redirectCache.evict(code);
    }

    private ShortUrlResponse validateIdempotentReplay(ShortUrlEntity existing, String requestHash) {
        if (!requestHash.equals(existing.getRequestHash())) {
            throw new ConflictException("IDEMPOTENCY_KEY_REUSED",
                    "Idempotency-Key was already used with a different request payload");
        }
        return toResponse(existing);
    }

    private String validateAliasSyntax(String alias) {
        if (!ALIAS.matcher(alias).matches()) {
            throw new com.agentic.sdlc.urlshortener.api.InvalidRequestException("customAlias",
                    "must be 4-32 characters using letters, digits, '_' or '-'");
        }
        return alias;
    }

    private String normalizeIdempotencyKey(String key) {
        if (key == null || key.isBlank()) return null;
        String normalized = key.trim();
        if (normalized.length() > 200) {
            throw new com.agentic.sdlc.urlshortener.api.InvalidRequestException("Idempotency-Key", "must be at most 200 characters");
        }
        return normalized;
    }

    private ShortUrlResponse toResponse(ShortUrlEntity entity) {
        Instant now = clock.instant();
        LinkStatus status = entity.getDeactivatedAt() != null
                ? LinkStatus.DEACTIVATED
                : entity.isExpired(now) ? LinkStatus.EXPIRED : LinkStatus.ACTIVE;
        return new ShortUrlResponse(
                entity.getShortCode(),
                properties.baseUrl().replaceAll("/+$", "") + "/" + entity.getShortCode(),
                entity.getDestinationUrl(),
                entity.getCreatedAt(),
                entity.getExpiresAt(),
                status == LinkStatus.ACTIVE,
                status
        );
    }
}
