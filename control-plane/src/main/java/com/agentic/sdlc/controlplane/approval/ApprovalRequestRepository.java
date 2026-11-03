package com.agentic.sdlc.controlplane.approval;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ApprovalRequestRepository extends JpaRepository<ApprovalRequestEntity, UUID> {
    Optional<ApprovalRequestEntity> findByWorkflowIdAndNodeKeyAndNodeGeneration(
            UUID workflowId, String nodeKey, int nodeGeneration);

    List<ApprovalRequestEntity> findByWorkflowIdAndStatus(UUID workflowId, ApprovalStatus status);
    List<ApprovalRequestEntity> findByWorkflowIdOrderByRequestedAtAsc(UUID workflowId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from ApprovalRequestEntity a where a.workflowId = :workflowId " +
            "and a.nodeKey = :nodeKey and a.nodeGeneration = :nodeGeneration")
    Optional<ApprovalRequestEntity> findForUpdateByWorkflowIdAndNodeKeyAndNodeGeneration(
            @Param("workflowId") UUID workflowId,
            @Param("nodeKey") String nodeKey,
            @Param("nodeGeneration") int nodeGeneration);
}
