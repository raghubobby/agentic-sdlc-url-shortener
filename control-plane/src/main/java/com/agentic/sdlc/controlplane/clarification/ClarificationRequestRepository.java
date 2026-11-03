package com.agentic.sdlc.controlplane.clarification;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ClarificationRequestRepository extends JpaRepository<ClarificationRequestEntity, UUID> {
    List<ClarificationRequestEntity> findByWorkflowIdOrderByRequestedAtAsc(UUID workflowId);
    boolean existsByWorkflowIdAndStatus(UUID workflowId, ClarificationStatus status);
    Optional<ClarificationRequestEntity> findByWorkflowIdAndPlanRevisionAndNodeKey(
            UUID workflowId, int planRevision, String nodeKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from ClarificationRequestEntity c where c.id = :id")
    Optional<ClarificationRequestEntity> findForUpdateById(@Param("id") UUID id);
}
