package com.agentic.sdlc.urlshortener.repository;

import com.agentic.sdlc.urlshortener.domain.ClickEventEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClickEventRepository extends JpaRepository<ClickEventEntity, UUID> {
    long countByShortUrlId(UUID shortUrlId);

    Optional<ClickEventEntity> findTopByShortUrlIdOrderByOccurredAtDesc(UUID shortUrlId);

    List<ClickEventEntity> findByShortUrlIdAndOccurredAtGreaterThanEqualOrderByOccurredAtAsc(
            UUID shortUrlId, Instant since);

    Page<ClickEventEntity> findByShortUrlIdOrderByOccurredAtDesc(UUID shortUrlId, Pageable pageable);

    @Query("""
            select c.referrerHost, count(c)
            from ClickEventEntity c
            where c.shortUrlId = :shortUrlId and c.referrerHost is not null
            group by c.referrerHost
            order by count(c) desc
            """)
    List<Object[]> referrerCounts(@Param("shortUrlId") UUID shortUrlId);

    @Query("""
            select c.userAgentFamily, count(c)
            from ClickEventEntity c
            where c.shortUrlId = :shortUrlId and c.userAgentFamily is not null
            group by c.userAgentFamily
            order by count(c) desc
            """)
    List<Object[]> browserCounts(@Param("shortUrlId") UUID shortUrlId);
}
