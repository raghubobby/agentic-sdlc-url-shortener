package com.agentic.sdlc.controlplane.approval;

import com.agentic.sdlc.controlplane.governance.RiskLevel;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_request",
        uniqueConstraints = @UniqueConstraint(name = "uk_workflow_approval_generation",
                columnNames = {"workflow_id", "node_key", "node_generation"}))
public class ApprovalRequestEntity {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_key", nullable = false, length = 120)
    private String nodeKey;

    @Column(name = "node_generation", nullable = false)
    private int nodeGeneration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 30)
    private RiskLevel riskLevel;

    @Column(name = "required_approvals", nullable = false)
    private int requiredApprovals;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    protected ApprovalRequestEntity() {}

    public ApprovalRequestEntity(
            UUID workflowId, String nodeKey, int nodeGeneration,
            RiskLevel riskLevel, int requiredApprovals, String reason) {
        this.id = UUID.randomUUID();
        this.workflowId = workflowId;
        this.nodeKey = nodeKey;
        this.nodeGeneration = nodeGeneration;
        this.status = ApprovalStatus.PENDING;
        this.riskLevel = riskLevel;
        this.requiredApprovals = requiredApprovals;
        this.reason = reason;
        this.requestedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getNodeKey() { return nodeKey; }
    public int getNodeGeneration() { return nodeGeneration; }
    public ApprovalStatus getStatus() { return status; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public int getRequiredApprovals() { return requiredApprovals; }
    public String getReason() { return reason; }
    public Instant getRequestedAt() { return requestedAt; }
    public Instant getResolvedAt() { return resolvedAt; }

    public void approve() {
        this.status = ApprovalStatus.APPROVED;
        this.resolvedAt = Instant.now();
    }

    public void reject() {
        this.status = ApprovalStatus.REJECTED;
        this.resolvedAt = Instant.now();
    }

    public void supersede() {
        if (this.status == ApprovalStatus.PENDING) {
            this.status = ApprovalStatus.SUPERSEDED;
            this.resolvedAt = Instant.now();
        }
    }
}
