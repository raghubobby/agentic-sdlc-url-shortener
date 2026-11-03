package com.agentic.sdlc.controlplane.replan;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "replan_record",
        uniqueConstraints = @UniqueConstraint(name = "uk_replan_change_id", columnNames = {"workflow_id", "change_id"}))
public class ReplanRecordEntity {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "change_id", nullable = false, length = 160)
    private String changeId;

    @Column(name = "from_revision", nullable = false)
    private int fromRevision;

    @Column(name = "to_revision", nullable = false)
    private int toRevision;

    @Column(name = "source_node_key", nullable = false, length = 120)
    private String sourceNodeKey;

    @Column(name = "previous_hash", nullable = false, length = 64)
    private String previousHash;

    @Column(name = "new_hash", nullable = false, length = 64)
    private String newHash;

    @Column(nullable = false)
    private boolean applied;

    @Column(name = "lineage_impacted_nodes", nullable = false, columnDefinition = "text")
    private String lineageImpactedNodes;

    @Column(name = "invalidated_nodes", nullable = false, columnDefinition = "text")
    private String invalidatedNodes;

    @Column(name = "preserved_nodes", nullable = false, columnDefinition = "text")
    private String preservedNodes;

    @Column(name = "next_ready_nodes", nullable = false, columnDefinition = "text")
    private String nextReadyNodes;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "changed_by", nullable = false, length = 160)
    private String changedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReplanRecordEntity() {}

    public ReplanRecordEntity(
            UUID workflowId,
            String changeId,
            int fromRevision,
            int toRevision,
            String sourceNodeKey,
            String previousHash,
            String newHash,
            boolean applied,
            String lineageImpactedNodes,
            String invalidatedNodes,
            String preservedNodes,
            String nextReadyNodes,
            String reason,
            String changedBy) {
        this.id = UUID.randomUUID();
        this.workflowId = workflowId;
        this.changeId = changeId;
        this.fromRevision = fromRevision;
        this.toRevision = toRevision;
        this.sourceNodeKey = sourceNodeKey;
        this.previousHash = previousHash;
        this.newHash = newHash;
        this.applied = applied;
        this.lineageImpactedNodes = lineageImpactedNodes;
        this.invalidatedNodes = invalidatedNodes;
        this.preservedNodes = preservedNodes;
        this.nextReadyNodes = nextReadyNodes;
        this.reason = reason;
        this.changedBy = changedBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getChangeId() { return changeId; }
    public int getFromRevision() { return fromRevision; }
    public int getToRevision() { return toRevision; }
    public String getSourceNodeKey() { return sourceNodeKey; }
    public String getPreviousHash() { return previousHash; }
    public String getNewHash() { return newHash; }
    public boolean isApplied() { return applied; }
    public String getLineageImpactedNodes() { return lineageImpactedNodes; }
    public String getInvalidatedNodes() { return invalidatedNodes; }
    public String getPreservedNodes() { return preservedNodes; }
    public String getNextReadyNodes() { return nextReadyNodes; }
    public String getReason() { return reason; }
    public String getChangedBy() { return changedBy; }
    public Instant getCreatedAt() { return createdAt; }
}
