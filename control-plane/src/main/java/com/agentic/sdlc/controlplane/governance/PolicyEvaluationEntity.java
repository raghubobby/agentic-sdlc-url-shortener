package com.agentic.sdlc.controlplane.governance;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "policy_evaluation")
public class PolicyEvaluationEntity {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_key", nullable = false, length = 120)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 30)
    private RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private PolicyDecision decision;

    @Column(name = "required_approvals", nullable = false)
    private int requiredApprovals;

    @Column(nullable = false, columnDefinition = "text")
    private String reason;

    @Column(name = "risk_reasons", nullable = false, columnDefinition = "text")
    private String riskReasons;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PolicyEvaluationEntity() {}

    public PolicyEvaluationEntity(UUID workflowId, String nodeKey, PolicyOutcome outcome) {
        this.id = UUID.randomUUID();
        this.workflowId = workflowId;
        this.nodeKey = nodeKey;
        this.riskLevel = outcome.risk().level();
        this.riskScore = outcome.risk().score();
        this.decision = outcome.decision();
        this.requiredApprovals = outcome.requiredApprovals();
        this.reason = outcome.reason();
        this.riskReasons = String.join(",", outcome.risk().reasons());
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getNodeKey() { return nodeKey; }
    public RiskLevel getRiskLevel() { return riskLevel; }
    public int getRiskScore() { return riskScore; }
    public PolicyDecision getDecision() { return decision; }
    public int getRequiredApprovals() { return requiredApprovals; }
    public String getReason() { return reason; }
    public String getRiskReasons() { return riskReasons; }
    public Instant getCreatedAt() { return createdAt; }
}
