package com.agentic.sdlc.controlplane.workflow;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "workflow_run")
public class WorkflowRunEntity {
    @Id
    private UUID id;

    @Column(nullable = false, length = 100)
    private String scenario;

    @Column(nullable = false, columnDefinition = "text")
    private String requirement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private WorkflowStatus status;

    @Column(name = "plan_revision", nullable = false)
    private int planRevision;

    @Column(name = "base_commit", nullable = false, length = 80)
    private String baseCommit;

    @Version
    @Column(name = "lock_version", nullable = false)
    private long lockVersion;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected WorkflowRunEntity() {
    }

    public WorkflowRunEntity(UUID id, String scenario, String requirement, String baseCommit) {
        this.id = id;
        this.scenario = scenario;
        this.requirement = requirement;
        this.baseCommit = baseCommit;
        this.status = WorkflowStatus.CREATED;
        this.planRevision = 1;
    }

    public UUID getId() { return id; }
    public String getScenario() { return scenario; }
    public String getRequirement() { return requirement; }
    public WorkflowStatus getStatus() { return status; }
    public int getPlanRevision() { return planRevision; }
    public String getBaseCommit() { return baseCommit; }
    public long getLockVersion() { return lockVersion; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }

    public void markRunning() {
        this.status = WorkflowStatus.RUNNING;
        if (this.startedAt == null) {
            this.startedAt = Instant.now();
        }
    }

    public void markWaitingForApproval() {
        this.status = WorkflowStatus.WAITING_FOR_APPROVAL;
    }

    public void markWaitingForClarification() {
        this.status = WorkflowStatus.WAITING_FOR_CLARIFICATION;
    }

    public void markCompleted() {
        this.status = WorkflowStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void markFailed() {
        this.status = WorkflowStatus.FAILED;
        this.completedAt = Instant.now();
    }

    public int beginReplan(String revisedRequirement) {
        this.planRevision++;
        if (revisedRequirement != null) this.requirement = revisedRequirement;
        this.status = WorkflowStatus.RUNNING;
        this.completedAt = null;
        return this.planRevision;
    }
}
