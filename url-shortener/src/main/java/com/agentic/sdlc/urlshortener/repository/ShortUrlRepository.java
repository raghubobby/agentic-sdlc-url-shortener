package com.agentic.sdlc.urlshortener.repository;

import com.agentic.sdlc.urlshortener.domain.ShortUrlEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShortUrlRepository extends JpaRepository<ShortUrlEntity, UUID> {
    Optional<ShortUrlEntity> findByShortCode(String shortCode);
    Optional<ShortUrlEntity> findByIdempotencyKey(String idempotencyKey);
}
