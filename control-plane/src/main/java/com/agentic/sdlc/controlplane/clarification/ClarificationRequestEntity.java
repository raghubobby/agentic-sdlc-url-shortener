package com.agentic.sdlc.controlplane.clarification;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clarification_request",
        uniqueConstraints = @UniqueConstraint(name = "uk_clarification_revision_node",
                columnNames = {"workflow_id", "plan_revision", "node_key"}))
public class ClarificationRequestEntity {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "plan_revision", nullable = false)
    private int planRevision;

    @Column(name = "node_key", nullable = false, length = 120)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ClarificationStatus status;

    @Column(nullable = false, columnDefinition = "text")
    private String questions;

    @Column(columnDefinition = "text")
    private String answer;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by", length = 200)
    private String resolvedBy;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected ClarificationRequestEntity() {}

    public ClarificationRequestEntity(UUID workflowId, int planRevision, String nodeKey, String questions) {
        this.id = UUID.randomUUID();
        this.workflowId = workflowId;
        this.planRevision = planRevision;
        this.nodeKey = nodeKey;
        this.questions = questions;
        this.status = ClarificationStatus.OPEN;
        this.requestedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public int getPlanRevision() { return planRevision; }
    public String getNodeKey() { return nodeKey; }
    public ClarificationStatus getStatus() { return status; }
    public String getQuestions() { return questions; }
    public String getAnswer() { return answer; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getResolvedAt() { return resolvedAt; }
    public String getResolvedBy() { return resolvedBy; }

    public void resolve(String answer, String resolvedBy) {
        if (status != ClarificationStatus.OPEN) {
            throw new IllegalStateException("Clarification is not open: " + id);
        }
        this.answer = answer;
        this.resolvedBy = resolvedBy;
        this.resolvedAt = Instant.now();
        this.status = ClarificationStatus.RESOLVED;
    }

    public void supersede() {
        if (status == ClarificationStatus.OPEN) status = ClarificationStatus.SUPERSEDED;
    }
}
