package com.agentic.sdlc.controlplane.workflow;

import com.agentic.sdlc.controlplane.governance.RiskLevel;
import com.agentic.sdlc.controlplane.recovery.CompensationStatus;
import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_node_execution",
        uniqueConstraints = @UniqueConstraint(name = "uk_workflow_node", columnNames = {"workflow_id", "node_key"}))
public class WorkflowNodeExecutionEntity {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_key", nullable = false, length = 120)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private NodeStatus status;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", length = 30)
    private RiskLevel riskLevel;

    @Column(nullable = false)
    private int attempt;

    @Column(nullable = false)
    private int generation;

    @Column(name = "validated_revision", nullable = false)
    private int validatedRevision;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_category", length = 40)
    private FailureCategory failureCategory;

    @Column(name = "last_error", columnDefinition = "text")
    private String lastError;

    @Enumerated(EnumType.STRING)
    @Column(name = "compensation_status", nullable = false, length = 30)
    private CompensationStatus compensationStatus;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected WorkflowNodeExecutionEntity() {}

    public WorkflowNodeExecutionEntity(UUID id, UUID workflowId, String nodeKey) {
        this.id = id;
        this.workflowId = workflowId;
        this.nodeKey = nodeKey;
        this.requiresApproval = false;
        this.status = NodeStatus.PENDING;
        this.attempt = 0;
        this.generation = 1;
        this.validatedRevision = 0;
        this.compensationStatus = CompensationStatus.NONE;
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getNodeKey() { return nodeKey; }
    public NodeStatus getStatus() { return status; }
    public boolean isRequiresApproval() { return requiresApproval; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public int getAttempt() { return attempt; }
    public int getGeneration() { return generation; }
    public int getValidatedRevision() { return validatedRevision; }
    public FailureCategory getFailureCategory() { return failureCategory; }
    public String getLastError() { return lastError; }
    public CompensationStatus getCompensationStatus() { return compensationStatus; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void setRequiresApproval(boolean requiresApproval) { this.requiresApproval = requiresApproval; }
    public void setRiskLevel(RiskLevel riskLevel) { this.riskLevel = riskLevel; }

    public void markRunning() {
        this.status = NodeStatus.RUNNING;
        this.attempt++;
        if (this.startedAt == null) this.startedAt = Instant.now();
        this.lastError = null;
        this.failureCategory = null;
    }

    public void markWaitingForApproval() { this.status = NodeStatus.WAITING_FOR_APPROVAL; }

    public void markPendingAfterApproval() {
        this.status = NodeStatus.PENDING;
        this.completedAt = null;
    }

    public void markRetryPending(FailureCategory category, String error) {
        this.status = NodeStatus.PENDING;
        this.failureCategory = category;
        this.lastError = error;
    }

    public void markSucceeded(int planRevision) {
        this.status = NodeStatus.SUCCEEDED;
        this.completedAt = Instant.now();
        this.validatedRevision = planRevision;
        this.failureCategory = null;
        this.lastError = null;
    }

    public void markExternallyRevised(int planRevision) {
        this.status = NodeStatus.SUCCEEDED;
        this.generation++;
        this.attempt = 0;
        this.validatedRevision = planRevision;
        this.requiresApproval = false;
        this.riskLevel = null;
        this.failureCategory = null;
        this.lastError = null;
        this.compensationStatus = CompensationStatus.NONE;
        this.startedAt = null;
        this.completedAt = Instant.now();
    }

    public void resetForReplan() {
        this.status = NodeStatus.PENDING;
        this.generation++;
        this.attempt = 0;
        this.requiresApproval = false;
        this.riskLevel = null;
        this.failureCategory = null;
        this.lastError = null;
        this.compensationStatus = CompensationStatus.NONE;
        this.startedAt = null;
        this.completedAt = null;
    }

    public void markFailed(FailureCategory category, String error) {
        this.status = NodeStatus.FAILED;
        this.completedAt = Instant.now();
        this.failureCategory = category;
        this.lastError = error;
    }

    public void markCompensationRunning() { this.compensationStatus = CompensationStatus.RUNNING; }
    public void markCompensationSucceeded() { this.compensationStatus = CompensationStatus.SUCCEEDED; }
    public void markCompensationFailed() { this.compensationStatus = CompensationStatus.FAILED; }
}
