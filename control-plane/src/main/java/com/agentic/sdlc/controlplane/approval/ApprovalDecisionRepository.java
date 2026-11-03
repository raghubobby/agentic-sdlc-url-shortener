package com.agentic.sdlc.controlplane.approval;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecisionEntity, UUID> {
    boolean existsByApprovalRequestIdAndActor(UUID approvalRequestId, String actor);
    Optional<ApprovalDecisionEntity> findByApprovalRequestIdAndActor(UUID approvalRequestId, String actor);
    long countByApprovalRequestIdAndDecision(UUID approvalRequestId, ApprovalVote decision);
    List<ApprovalDecisionEntity> findByApprovalRequestIdOrderByDecidedAtAsc(UUID approvalRequestId);
}
