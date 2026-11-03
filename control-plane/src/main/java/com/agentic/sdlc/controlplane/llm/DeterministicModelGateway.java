package com.agentic.sdlc.controlplane.llm;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import com.agentic.sdlc.controlplane.tool.ToolAction;
import com.agentic.sdlc.controlplane.tool.ToolActionType;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.util.List;
import java.util.Locale;

@Component
public class DeterministicModelGateway {
    private final ObjectMapper objectMapper;

    public DeterministicModelGateway(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ModelResponse generate(ModelRequest request) {
        long started = System.nanoTime();
        String scenario = scenario(request);

        ObjectNode output = objectMapper.createObjectNode();
        output.put("summary", summary(request, scenario));
        output.put("confidence", 0.95);
        output.set("assumptions", objectMapper.valueToTree(assumptions(request, scenario)));
        output.set("ambiguities", objectMapper.valueToTree(deterministicAmbiguities(request, scenario)));
        output.set("decisions", objectMapper.valueToTree(decisions(request, scenario)));
        output.set("risks", objectMapper.valueToTree(risks(request, scenario)));
        output.set("requestedCapabilities", objectMapper.valueToTree(requestedCapabilities(request.nodeKey(), scenario)));
        output.set("evidence", objectMapper.valueToTree(List.of(
                "workflowId=" + request.workflowId(),
                "scenario=" + scenario,
                "node=" + request.nodeKey())));
        output.set("toolActions", objectMapper.valueToTree(toolActions(request.nodeKey(), scenario)));

        return new ModelResponse(
                request.requestId(), "deterministic", "fixture-v3", output,
                false, null, elapsedMillis(started), 0, 0);
    }

    private String scenario(ModelRequest request) {
        String task = request.prompt().task().toLowerCase(Locale.ROOT);
        if (task.contains("scenario: greenfield")) return "greenfield";
        if (task.contains("scenario: brownfield")) return "brownfield";
        if (task.contains("scenario: ambiguous")) return "ambiguous";
        return "custom";
    }

    private String summary(ModelRequest request, String scenario) {
        String node = request.nodeKey();
        return switch (scenario) {
            case "greenfield" -> switch (node) {
                case "requirements" -> "Normalized the single-use link capability with backward compatibility, 410-after-consumption, concurrency safety, and cache correctness as explicit acceptance criteria.";
                case "planning" -> "Decomposed the new capability into API/domain implementation and an independent schema migration that can execute in parallel before validation and security synchronization.";
                case "architecture" -> "Selected database-authoritative one-time consumption: single-use redirects are never cached and are consumed through an atomic conditional database update to prevent concurrent double use.";
                case "implementation-api" -> "Prepared the application-layer single-use implementation plus an end-to-end regression test proving first redirect succeeds and the second returns 410 Gone.";
                case "implementation-persistence" -> "Prepared the additive PostgreSQL migration for single_use and consumed_at without changing existing multi-use rows.";
                case "validation-tests" -> "Validated the assembled greenfield changes through compilation and the URL-shortener test suite.";
                case "security-review" -> "Reviewed the assembled greenfield workspace with deterministic security scanning before release approval.";
                default -> "Produced governed release evidence for the greenfield single-use link capability.";
            };
            case "brownfield" -> switch (node) {
                case "requirements" -> "Normalized the incident into a correctness constraint: Redis may optimize redirects but must never override PostgreSQL state for expired or deactivated links.";
                case "planning" -> "Constrained the brownfield change to the redirect read path and a regression test; no schema rewrite is justified by repository evidence.";
                case "architecture" -> "Kept PostgreSQL authoritative on cache hits and retained Redis as a best-effort optimization, explicitly trading one database read for correctness under stale cache state.";
                case "implementation-api" -> "Prepared the smallest safe redirect-path patch and a regression test showing stale cache cannot resurrect a deactivated link.";
                case "implementation-persistence" -> "Repository analysis found no persistence schema change is required; preserving the existing schema is the safer brownfield decision.";
                case "validation-tests" -> "Validated the targeted brownfield change and regression evidence in the assembled workspace.";
                case "security-review" -> "Scanned the targeted brownfield workspace for high-signal security regressions.";
                default -> "Produced governed release evidence for the brownfield redirect-correctness fix.";
            };
            case "ambiguous" -> switch (node) {
                case "requirements" -> request.prompt().task().contains("Human clarification (authoritative):")
                        ? "Accepted the authoritative human clarification and converted the previously ambiguous security request into a bounded, reviewable engineering requirement."
                        : "Detected material ambiguity in 'more secure' and safe-stopped instead of inventing an enterprise threat model.";
                case "planning" -> "Replanned only downstream work after the human clarification; repository evidence is used to avoid unnecessary implementation when existing controls already satisfy the clarified requirement.";
                case "architecture" -> "Mapped the clarified security requirement to existing URL validation and governed evidence, favoring validation/documentation over speculative rewrites.";
                case "implementation-api", "implementation-persistence" -> "No product mutation is justified by the clarified requirement and current repository evidence; preserved the implementation and advanced with validation evidence instead.";
                case "validation-tests" -> "Revalidated the current product after clarification to prove the bounded security controls remain intact.";
                case "security-review" -> "Executed deterministic security scanning after clarification and replanning.";
                default -> "Produced governed release evidence after ambiguity safe-stop, human clarification, and selective replanning.";
            };
            default -> "Deterministic engineering analysis for " + node;
        };
    }

    private List<String> assumptions(ModelRequest request, String scenario) {
        if ("brownfield".equals(scenario)) {
            return List.of(
                    "PostgreSQL is the source of truth for redirect lifecycle state",
                    "Redis is an optimization and must fail open to the database",
                    "The safest brownfield change minimizes the modified surface area");
        }
        if ("greenfield".equals(scenario)) {
            return List.of(
                    "Existing multi-use links must preserve current behavior",
                    "Single-use consumption requires an atomic database operation under concurrency",
                    "Agent changes remain isolated in Git worktrees until human-approved release readiness");
        }
        if ("ambiguous".equals(scenario)) {
            return List.of(
                    "The model must not choose an enterprise threat model on behalf of the human reviewer",
                    "After clarification, unchanged code is preferable when repository evidence already satisfies the bounded requirement");
        }
        return List.of("Inputs are limited to persisted workflow context and governed repository evidence");
    }

    private List<String> deterministicAmbiguities(ModelRequest request, String scenario) {
        String task = request.prompt().task();
        if (!"ambiguous".equals(scenario) || !"requirements".equals(request.nodeKey())) return List.of();
        if (task.contains("Human clarification (authoritative):")) return List.of();
        return List.of(
                "What threat model should 'more secure' address: enumeration, unauthorized redirect access, malicious destinations, analytics exposure, or all of these?",
                "Should enterprise links require authenticated access, tenant isolation, stricter destination validation, or remain publicly redirectable?",
                "What backward-compatibility constraints apply to existing public links and clients?");
    }

    private List<String> decisions(ModelRequest request, String scenario) {
        String node = request.nodeKey();
        if ("greenfield".equals(scenario) && "architecture".equals(node)) {
            return List.of(
                    "Do not cache single-use links",
                    "Guarantee one-time consumption with an atomic conditional database update",
                    "Keep schema evolution additive and backward compatible");
        }
        if ("brownfield".equals(scenario) && "implementation-persistence".equals(node)) {
            return List.of(
                    "Reject an unnecessary schema change",
                    "Keep the fix in the redirect read path where stale cache state is consumed");
        }
        if ("ambiguous".equals(scenario) && !request.prompt().task().contains("Human clarification (authoritative):")) {
            return List.of("Safe-stop before planning or implementation until a human defines the security objective");
        }
        return List.of("Preserve deterministic governance and execution authority outside the model runtime");
    }

    private List<String> risks(ModelRequest request, String scenario) {
        if ("greenfield".equals(scenario)) {
            return List.of(
                    "Concurrent redirects could consume a one-time link more than once without atomic database enforcement",
                    "Caching a single-use redirect could bypass consumed state",
                    "Schema and application changes must remain backward compatible");
        }
        if ("brownfield".equals(scenario)) {
            return List.of(
                    "Stale Redis state can resurrect deactivated or expired links",
                    "Authoritative database verification on cache hits increases database read load",
                    "A broad cache rewrite would create unnecessary brownfield risk");
        }
        if ("ambiguous".equals(scenario)) {
            return List.of(
                    "Coding before clarification could implement the wrong security control",
                    "Replanning must invalidate only downstream evidence affected by the clarified requirement");
        }
        return List.of("Model output must not bypass policy, evidence, tool authorization, or human approval gates");
    }

    private List<String> requestedCapabilities(String nodeKey, String scenario) {
        if (nodeKey.startsWith("implementation-")) {
            boolean changesProduct = "greenfield".equals(scenario)
                    || ("brownfield".equals(scenario) && "implementation-api".equals(nodeKey));
            if (!changesProduct) {
                return List.of(
                        AgentCapability.READ_REQUIREMENT.name(),
                        AgentCapability.READ_ARTIFACTS.name(),
                        AgentCapability.READ_REPOSITORY.name(),
                        AgentCapability.WRITE_ENGINEERING_ARTIFACT.name());
            }
            return List.of(
                    AgentCapability.READ_REQUIREMENT.name(),
                    AgentCapability.READ_ARTIFACTS.name(),
                    AgentCapability.READ_REPOSITORY.name(),
                    AgentCapability.WRITE_ENGINEERING_ARTIFACT.name(),
                    AgentCapability.WRITE_WORKTREE.name(),
                    AgentCapability.RUN_BUILD.name(),
                    AgentCapability.RUN_TESTS.name(),
                    AgentCapability.GENERATE_DIFF.name());
        }
        if (nodeKey.equals("security-review")) {
            return List.of(
                    AgentCapability.READ_REQUIREMENT.name(),
                    AgentCapability.READ_ARTIFACTS.name(),
                    AgentCapability.READ_REPOSITORY.name(),
                    AgentCapability.RUN_SECURITY_SCAN.name(),
                    AgentCapability.WRITE_ENGINEERING_ARTIFACT.name());
        }
        if (nodeKey.equals("validation-tests")) {
            return List.of(
                    AgentCapability.READ_REQUIREMENT.name(),
                    AgentCapability.READ_ARTIFACTS.name(),
                    AgentCapability.READ_REPOSITORY.name(),
                    AgentCapability.RUN_BUILD.name(),
                    AgentCapability.RUN_TESTS.name(),
                    AgentCapability.WRITE_ENGINEERING_ARTIFACT.name());
        }
        if (nodeKey.equals("release-approval")) {
            return List.of(
                    AgentCapability.READ_REQUIREMENT.name(),
                    AgentCapability.READ_ARTIFACTS.name(),
                    AgentCapability.REQUEST_HUMAN_REVIEW.name(),
                    AgentCapability.WRITE_ENGINEERING_ARTIFACT.name());
        }
        return List.of(
                AgentCapability.READ_REQUIREMENT.name(),
                AgentCapability.READ_ARTIFACTS.name(),
                AgentCapability.READ_REPOSITORY.name(),
                AgentCapability.WRITE_ENGINEERING_ARTIFACT.name());
    }

    private List<ToolAction> parallelChangeActions(String patch) {
        return List.of(
                new ToolAction(ToolActionType.APPLY_PATCH, null, patch, null),
                new ToolAction(ToolActionType.GENERATE_DIFF, null, null, null));
    }

    private List<ToolAction> toolActions(String nodeKey, String scenario) {
        if ("greenfield".equals(scenario) && nodeKey.equals("implementation-api")) {
            return parallelChangeActions(greenfieldApiPatch());
        }
        if ("greenfield".equals(scenario) && nodeKey.equals("implementation-persistence")) {
            return parallelChangeActions(greenfieldPersistencePatch());
        }
        if ("brownfield".equals(scenario) && nodeKey.equals("implementation-api")) {
            return changeActions(brownfieldApiPatch());
        }
        if (nodeKey.equals("validation-tests")) {
            return List.of(
                    new ToolAction(ToolActionType.RUN_BUILD, null, null, "url-shortener"),
                    new ToolAction(ToolActionType.RUN_TESTS, null, null, "url-shortener"));
        }
        if (nodeKey.equals("security-review")) {
            return List.of(new ToolAction(ToolActionType.RUN_SECURITY_SCAN, null, null, "url-shortener"));
        }
        return List.of();
    }

    private List<ToolAction> changeActions(String patch) {
        return List.of(
                new ToolAction(ToolActionType.APPLY_PATCH, null, patch, null),
                new ToolAction(ToolActionType.RUN_BUILD, null, null, "url-shortener"),
                new ToolAction(ToolActionType.RUN_TESTS, null, null, "url-shortener"),
                new ToolAction(ToolActionType.GENERATE_DIFF, null, null, null));
    }

    private String greenfieldApiPatch() {
        return """
                diff --git a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/api/CreateShortUrlRequest.java b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/api/CreateShortUrlRequest.java
                index b22dc15..7ae9de1 100644
                --- a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/api/CreateShortUrlRequest.java
                +++ b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/api/CreateShortUrlRequest.java
                @@ -8,5 +8,14 @@ import java.time.Instant;
                 public record CreateShortUrlRequest(
                         @NotBlank @Size(max = 4096) String url,
                         @Size(max = 32) String customAlias,
                -        Instant expiresAt
                -) {}
                +        Instant expiresAt,
                +        Boolean singleUse
                +) {
                +    public CreateShortUrlRequest {
                +        singleUse = Boolean.TRUE.equals(singleUse);
                +    }
                +
                +    public CreateShortUrlRequest(String url, String customAlias, Instant expiresAt) {
                +        this(url, customAlias, expiresAt, false);
                +    }
                +}
                diff --git a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/api/LinkStatus.java b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/api/LinkStatus.java
                index f94a971..af30f26 100644
                --- a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/api/LinkStatus.java
                +++ b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/api/LinkStatus.java
                @@ -3,5 +3,6 @@ package com.agentic.sdlc.urlshortener.api;
                 public enum LinkStatus {
                     ACTIVE,
                     EXPIRED,
                -    DEACTIVATED
                +    DEACTIVATED,
                +    CONSUMED
                 }
                diff --git a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/domain/ShortUrlEntity.java b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/domain/ShortUrlEntity.java
                index 5d6aae9..a0743bc 100644
                --- a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/domain/ShortUrlEntity.java
                +++ b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/domain/ShortUrlEntity.java
                @@ -30,6 +30,12 @@ public class ShortUrlEntity {
                     @Column(name = "deactivated_at")
                     private Instant deactivatedAt;
                \s
                +    @Column(name = "single_use", nullable = false)
                +    private boolean singleUse;
                +
                +    @Column(name = "consumed_at")
                +    private Instant consumedAt;
                +
                     @Column(name = "idempotency_key", unique = true, length = 200)
                     private String idempotencyKey;
                \s
                @@ -43,6 +49,11 @@ public class ShortUrlEntity {
                \s
                     public ShortUrlEntity(UUID id, String shortCode, String destinationUrl, Instant createdAt,
                                           Instant expiresAt, String idempotencyKey, String requestHash) {
                +        this(id, shortCode, destinationUrl, createdAt, expiresAt, idempotencyKey, requestHash, false);
                +    }
                +
                +    public ShortUrlEntity(UUID id, String shortCode, String destinationUrl, Instant createdAt,
                +                          Instant expiresAt, String idempotencyKey, String requestHash, boolean singleUse) {
                         this.id = id;
                         this.shortCode = shortCode;
                         this.destinationUrl = destinationUrl;
                @@ -50,6 +61,7 @@ public class ShortUrlEntity {
                         this.expiresAt = expiresAt;
                         this.idempotencyKey = idempotencyKey;
                         this.requestHash = requestHash;
                +        this.singleUse = singleUse;
                     }
                \s
                     public UUID getId() { return id; }
                @@ -58,6 +70,8 @@ public class ShortUrlEntity {
                     public Instant getCreatedAt() { return createdAt; }
                     public Instant getExpiresAt() { return expiresAt; }
                     public Instant getDeactivatedAt() { return deactivatedAt; }
                +    public boolean isSingleUse() { return singleUse; }
                +    public Instant getConsumedAt() { return consumedAt; }
                     public String getIdempotencyKey() { return idempotencyKey; }
                     public String getRequestHash() { return requestHash; }
                     public long getVersion() { return version; }
                @@ -66,8 +80,22 @@ public class ShortUrlEntity {
                         return expiresAt != null && !expiresAt.isAfter(now);
                     }
                \s
                +    public boolean isConsumed() {
                +        return singleUse && consumedAt != null;
                +    }
                +
                     public boolean isActive(Instant now) {
                -        return deactivatedAt == null && !isExpired(now);
                +        return deactivatedAt == null && !isExpired(now) && !isConsumed();
                +    }
                +
                +    public void consume(Instant now) {
                +        if (!singleUse) {
                +            throw new IllegalStateException("Only single-use links can be consumed");
                +        }
                +        if (consumedAt != null) {
                +            throw new IllegalStateException("Single-use link is already consumed");
                +        }
                +        consumedAt = now;
                     }
                \s
                     public void deactivate(Instant now) {
                diff --git a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/repository/ShortUrlRepository.java b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/repository/ShortUrlRepository.java
                index ac22c1f..148be0a 100644
                --- a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/repository/ShortUrlRepository.java
                +++ b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/repository/ShortUrlRepository.java
                @@ -2,11 +2,23 @@ package com.agentic.sdlc.urlshortener.repository;
                \s
                 import com.agentic.sdlc.urlshortener.domain.ShortUrlEntity;
                 import org.springframework.data.jpa.repository.JpaRepository;
                +import org.springframework.data.jpa.repository.Modifying;
                +import org.springframework.data.jpa.repository.Query;
                +import org.springframework.data.repository.query.Param;
                +import org.springframework.transaction.annotation.Transactional;
                \s
                +import java.time.Instant;
                 import java.util.Optional;
                 import java.util.UUID;
                \s
                 public interface ShortUrlRepository extends JpaRepository<ShortUrlEntity, UUID> {
                     Optional<ShortUrlEntity> findByShortCode(String shortCode);
                     Optional<ShortUrlEntity> findByIdempotencyKey(String idempotencyKey);
                +
                +    @Modifying(clearAutomatically = true, flushAutomatically = true)
                +    @Transactional
                +    @Query("update ShortUrlEntity s set s.consumedAt = :now " +
                +            "where s.shortCode = :shortCode and s.singleUse = true and s.consumedAt is null " +
                +            "and s.deactivatedAt is null and (s.expiresAt is null or s.expiresAt > :now)")
                +    int consumeSingleUseIfAvailable(@Param("shortCode") String shortCode, @Param("now") Instant now);
                 }
                diff --git a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RedirectService.java b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RedirectService.java
                index d0b48fa..07f908b 100644
                --- a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RedirectService.java
                +++ b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RedirectService.java
                @@ -8,7 +8,6 @@ import io.micrometer.core.instrument.Counter;
                 import io.micrometer.core.instrument.MeterRegistry;
                 import org.springframework.dao.DataAccessException;
                 import org.springframework.stereotype.Service;
                -import org.springframework.transaction.annotation.Transactional;
                \s
                 import java.time.Clock;
                 import java.time.Instant;
                @@ -33,7 +32,6 @@ public class RedirectService {
                         this.analyticsFailures = Counter.builder("url.analytics.record.failure").register(meterRegistry);
                     }
                \s
                -    @Transactional(readOnly = true)
                     public RedirectResult resolve(String code, String referrer, String userAgent) {
                         var cached = cache.get(code);
                         if (cached.isPresent()) {
                @@ -47,9 +45,22 @@ public class RedirectService {
                         }
                \s
                         cacheMisses.increment();
                -        ShortUrlEntity entity = repository.findByShortCode(code).orElseThrow(() -> new NotFoundException(code));
                -        ensureActive(entity, clock.instant());
                -        cache.put(code, entity.getId(), entity.getDestinationUrl(), entity.getExpiresAt());
                +        ShortUrlEntity entity = repository.findByShortCode(code)
                +                .orElseThrow(() -> new NotFoundException(code));
                +        Instant now = clock.instant();
                +        ensureActive(entity, now);
                +
                +        if (entity.isSingleUse()) {
                +            int consumed = repository.consumeSingleUseIfAvailable(code, now);
                +            if (consumed != 1) {
                +                cache.evict(code);
                +                throw new GoneException(code);
                +            }
                +            cache.evict(code);
                +        } else {
                +            cache.put(code, entity.getId(), entity.getDestinationUrl(), entity.getExpiresAt());
                +        }
                +
                         recordAnalytics(entity.getId(), referrer, userAgent);
                         return new RedirectResult(entity.getId(), entity.getDestinationUrl(), false);
                     }
                diff --git a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RequestHasher.java b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RequestHasher.java
                index 3106d89..6607335 100644
                --- a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RequestHasher.java
                +++ b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RequestHasher.java
                @@ -14,7 +14,8 @@ public class RequestHasher {
                     public String hash(CreateShortUrlRequest request) {
                         String canonical = request.url().trim() + "\\n" +
                                 normalize(request.customAlias()) + "\\n" +
                -                normalize(request.expiresAt());
                +                normalize(request.expiresAt()) + "\\n" +
                +                request.singleUse();
                         try {
                             return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                                     .digest(canonical.getBytes(StandardCharsets.UTF_8)));
                diff --git a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/ShortUrlService.java b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/ShortUrlService.java
                index 1a7a389..d781d53 100644
                --- a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/ShortUrlService.java
                +++ b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/ShortUrlService.java
                @@ -60,7 +60,8 @@ public class ShortUrlService {
                             throw new com.agentic.sdlc.urlshortener.api.InvalidRequestException("expiresAt", "must be in the future");
                         }
                         String normalizedKey = normalizeIdempotencyKey(idempotencyKey);
                -        String requestHash = requestHasher.hash(new CreateShortUrlRequest(normalizedUrl, request.customAlias(), request.expiresAt()));
                +        String requestHash = requestHasher.hash(new CreateShortUrlRequest(
                +                normalizedUrl, request.customAlias(), request.expiresAt(), request.singleUse()));
                \s
                         if (normalizedKey != null) {
                             var existing = repository.findByIdempotencyKey(normalizedKey);
                @@ -73,10 +74,12 @@ public class ShortUrlService {
                         for (int attempt = 1; attempt <= CODE_GENERATION_ATTEMPTS; attempt++) {
                             String code = customAlias ? requestedAlias : codeGenerator.generate(properties.codeLength());
                             ShortUrlEntity entity = new ShortUrlEntity(UUID.randomUUID(), code, normalizedUrl, now,
                -                    request.expiresAt(), normalizedKey, requestHash);
                +                    request.expiresAt(), normalizedKey, requestHash, request.singleUse());
                             try {
                                 entity = repository.saveAndFlush(entity);
                -                redirectCache.put(entity.getShortCode(), entity.getId(), entity.getDestinationUrl(), entity.getExpiresAt());
                +                if (!entity.isSingleUse()) {
                +                    redirectCache.put(entity.getShortCode(), entity.getId(), entity.getDestinationUrl(), entity.getExpiresAt());
                +                }
                                 return toResponse(entity);
                             } catch (DataIntegrityViolationException ex) {
                                 if (normalizedKey != null) {
                @@ -133,7 +136,9 @@ public class ShortUrlService {
                         Instant now = clock.instant();
                         LinkStatus status = entity.getDeactivatedAt() != null
                                 ? LinkStatus.DEACTIVATED
                -                : entity.isExpired(now) ? LinkStatus.EXPIRED : LinkStatus.ACTIVE;
                +                : entity.isExpired(now)
                +                ? LinkStatus.EXPIRED
                +                : entity.isConsumed() ? LinkStatus.CONSUMED : LinkStatus.ACTIVE;
                         return new ShortUrlResponse(
                                 entity.getShortCode(),
                                 properties.baseUrl().replaceAll("/+$", "") + "/" + entity.getShortCode(),
                diff --git a/url-shortener/src/test/java/com/agentic/sdlc/urlshortener/UrlShortenerH2Test.java b/url-shortener/src/test/java/com/agentic/sdlc/urlshortener/UrlShortenerH2Test.java
                index c3a3065..27986b4 100644
                --- a/url-shortener/src/test/java/com/agentic/sdlc/urlshortener/UrlShortenerH2Test.java
                +++ b/url-shortener/src/test/java/com/agentic/sdlc/urlshortener/UrlShortenerH2Test.java
                @@ -50,6 +50,22 @@ class UrlShortenerH2Test {
                                 .andExpect(jsonPath("$.events[0].userAgentFamily").value("curl"));
                     }
                \s
                +    @Test
                +    void singleUseLinkRedirectsExactlyOnce() throws Exception {
                +        mvc.perform(post("/api/v1/urls")
                +                        .contentType(MediaType.APPLICATION_JSON)
                +                        .content("{\\"url\\":\\"https://example.com/one-time\\",\\"customAlias\\":\\"onceonly\\",\\"singleUse\\":true}"))
                +                .andExpect(status().isCreated())
                +                .andExpect(jsonPath("$.code").value("onceonly"));
                +
                +        mvc.perform(get("/r/onceonly"))
                +                .andExpect(status().isFound())
                +                .andExpect(header().string("Location", "https://example.com/one-time"));
                +
                +        mvc.perform(get("/r/onceonly"))
                +                .andExpect(status().isGone());
                +    }
                +
                     @Test
                     void expirationContractAcceptsFutureAndRejectsPastTimestamps() throws Exception {
                         mvc.perform(post("/api/v1/urls")
                """;
    }

    private String greenfieldPersistencePatch() {
        return """
                diff --git a/url-shortener/src/main/resources/db/migration/V3__single_use_links.sql b/url-shortener/src/main/resources/db/migration/V3__single_use_links.sql
                new file mode 100644
                index 0000000..8470bf8
                --- /dev/null
                +++ b/url-shortener/src/main/resources/db/migration/V3__single_use_links.sql
                @@ -0,0 +1,8 @@
                +-- Greenfield capability: one-time redirect consumption.
                +ALTER TABLE url_shortener.short_url
                +    ADD COLUMN single_use boolean NOT NULL DEFAULT false,
                +    ADD COLUMN consumed_at timestamp with time zone null;
                +
                +CREATE INDEX idx_short_url_single_use_active
                +    ON url_shortener.short_url(short_code)
                +    WHERE single_use = true AND consumed_at IS NULL AND deactivated_at IS NULL;
                """;
    }

    private String brownfieldApiPatch() {
        return """
                diff --git a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RedirectService.java b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RedirectService.java
                index d0b48fa..cfcb1da 100644
                --- a/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RedirectService.java
                +++ b/url-shortener/src/main/java/com/agentic/sdlc/urlshortener/service/RedirectService.java
                @@ -12,6 +12,7 @@ import org.springframework.transaction.annotation.Transactional;
                \s
                 import java.time.Clock;
                 import java.time.Instant;
                +import java.util.Objects;
                \s
                 @Service
                 public class RedirectService {
                @@ -35,20 +36,30 @@ public class RedirectService {
                \s
                     @Transactional(readOnly = true)
                     public RedirectResult resolve(String code, String referrer, String userAgent) {
                +        Instant now = clock.instant();
                         var cached = cache.get(code);
                         if (cached.isPresent()) {
                             cacheHits.increment();
                -            if (cached.get().expiresAt() != null && !cached.get().expiresAt().isAfter(clock.instant())) {
                -                cache.evict(code);
                -                throw new GoneException(code);
                +
                +            ShortUrlEntity authoritative = repository.findById(cached.get().shortUrlId())
                +                    .orElseThrow(() -> {
                +                        cache.evict(code);
                +                        return new NotFoundException(code);
                +                    });
                +            ensureActive(authoritative, now);
                +
                +            if (!Objects.equals(authoritative.getDestinationUrl(), cached.get().destination())
                +                    || !Objects.equals(authoritative.getExpiresAt(), cached.get().expiresAt())) {
                +                cache.put(code, authoritative.getId(), authoritative.getDestinationUrl(), authoritative.getExpiresAt());
                             }
                -            recordAnalytics(cached.get().shortUrlId(), referrer, userAgent);
                -            return new RedirectResult(cached.get().shortUrlId(), cached.get().destination(), true);
                +
                +            recordAnalytics(authoritative.getId(), referrer, userAgent);
                +            return new RedirectResult(authoritative.getId(), authoritative.getDestinationUrl(), true);
                         }
                \s
                         cacheMisses.increment();
                         ShortUrlEntity entity = repository.findByShortCode(code).orElseThrow(() -> new NotFoundException(code));
                -        ensureActive(entity, clock.instant());
                +        ensureActive(entity, now);
                         cache.put(code, entity.getId(), entity.getDestinationUrl(), entity.getExpiresAt());
                         recordAnalytics(entity.getId(), referrer, userAgent);
                         return new RedirectResult(entity.getId(), entity.getDestinationUrl(), false);
                diff --git a/url-shortener/src/test/java/com/agentic/sdlc/urlshortener/service/RedirectServiceTest.java b/url-shortener/src/test/java/com/agentic/sdlc/urlshortener/service/RedirectServiceTest.java
                new file mode 100644
                index 0000000..6beb7ae
                --- /dev/null
                +++ b/url-shortener/src/test/java/com/agentic/sdlc/urlshortener/service/RedirectServiceTest.java
                @@ -0,0 +1,43 @@
                +package com.agentic.sdlc.urlshortener.service;
                +
                +import com.agentic.sdlc.urlshortener.api.GoneException;
                +import com.agentic.sdlc.urlshortener.domain.ShortUrlEntity;
                +import com.agentic.sdlc.urlshortener.repository.ShortUrlRepository;
                +import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
                +import org.junit.jupiter.api.Test;
                +
                +import java.time.Instant;
                +import java.util.Optional;
                +import java.util.UUID;
                +
                +import static org.assertj.core.api.Assertions.assertThatThrownBy;
                +import static org.mockito.Mockito.mock;
                +import static org.mockito.Mockito.verify;
                +import static org.mockito.Mockito.verifyNoInteractions;
                +import static org.mockito.Mockito.when;
                +
                +class RedirectServiceTest {
                +
                +    @Test
                +    void staleCacheCannotResurrectADeactivatedLink() {
                +        ShortUrlRepository repository = mock(ShortUrlRepository.class);
                +        RedirectCache cache = mock(RedirectCache.class);
                +        AnalyticsService analytics = mock(AnalyticsService.class);
                +        UUID id = UUID.randomUUID();
                +        ShortUrlEntity entity = new ShortUrlEntity(
                +                id, "stale1", "https://example.com/current", Instant.now(), null, null, null);
                +        entity.deactivate(Instant.now());
                +
                +        when(cache.get("stale1")).thenReturn(Optional.of(
                +                new RedirectCache.CachedRedirect(id, "https://example.com/stale", null)));
                +        when(repository.findById(id)).thenReturn(Optional.of(entity));
                +
                +        RedirectService service = new RedirectService(repository, cache, analytics, new SimpleMeterRegistry());
                +
                +        assertThatThrownBy(() -> service.resolve("stale1", null, "JUnit"))
                +                .isInstanceOf(GoneException.class);
                +
                +        verify(cache).evict("stale1");
                +        verifyNoInteractions(analytics);
                +    }
                +}
                """;
    }

    private long elapsedMillis(long started) {
        return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }
}
