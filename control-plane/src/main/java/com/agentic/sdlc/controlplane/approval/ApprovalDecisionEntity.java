package com.agentic.sdlc.controlplane.approval;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "approval_decision",
        uniqueConstraints = @UniqueConstraint(name = "uk_approval_actor", columnNames = {"approval_request_id", "actor"}))
public class ApprovalDecisionEntity {
    @Id
    private UUID id;

    @Column(name = "approval_request_id", nullable = false)
    private UUID approvalRequestId;

    @Column(nullable = false, length = 200)
    private String actor;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalVote decision;

    @Column(columnDefinition = "text")
    private String comment;

    @Column(name = "decided_at", nullable = false)
    private Instant decidedAt;

    protected ApprovalDecisionEntity() {}

    public ApprovalDecisionEntity(UUID approvalRequestId, String actor, ApprovalVote decision, String comment) {
        this.id = UUID.randomUUID();
        this.approvalRequestId = approvalRequestId;
        this.actor = actor;
        this.decision = decision;
        this.comment = comment;
        this.decidedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getApprovalRequestId() { return approvalRequestId; }
    public String getActor() { return actor; }
    public ApprovalVote getDecision() { return decision; }
    public String getComment() { return comment; }
    public Instant getDecidedAt() { return decidedAt; }
}
