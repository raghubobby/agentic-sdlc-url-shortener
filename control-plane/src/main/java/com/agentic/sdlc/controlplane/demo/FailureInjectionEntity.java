package com.agentic.sdlc.controlplane.demo;

import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "failure_injection")
public class FailureInjectionEntity {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_key", nullable = false, length = 120)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", nullable = false, length = 40)
    private FailureCategory failureCategory;

    @Column(name = "remaining_failures", nullable = false)
    private int remainingFailures;

    @Column(name = "side_effects_possible", nullable = false)
    private boolean sideEffectsPossible;

    @Column(nullable = false, columnDefinition = "text")
    private String message;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 160)
    private String createdBy;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected FailureInjectionEntity() {}

    public FailureInjectionEntity(UUID workflowId, String nodeKey, FailureCategory failureCategory,
                                  int remainingFailures, boolean sideEffectsPossible, String message, String createdBy) {
        this.id = UUID.randomUUID();
        this.workflowId = workflowId;
        this.nodeKey = nodeKey;
        this.failureCategory = failureCategory;
        this.remainingFailures = remainingFailures;
        this.sideEffectsPossible = sideEffectsPossible;
        this.message = message;
        this.createdBy = createdBy;
        this.active = true;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getNodeKey() { return nodeKey; }
    public FailureCategory getFailureCategory() { return failureCategory; }
    public int getRemainingFailures() { return remainingFailures; }
    public boolean isSideEffectsPossible() { return sideEffectsPossible; }
    public String getMessage() { return message; }
    public boolean isActive() { return active; }
    public Instant getCreatedAt() { return createdAt; }
    public String getCreatedBy() { return createdBy; }

    public void consume() {
        if (!active || remainingFailures <= 0) throw new IllegalStateException("Failure injection is exhausted");
        remainingFailures--;
        if (remainingFailures == 0) active = false;
    }

    public void clear() { active = false; remainingFailures = 0; }
}
