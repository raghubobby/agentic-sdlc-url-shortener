package com.agentic.sdlc.controlplane.workflow;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WorkflowRunRepository extends JpaRepository<WorkflowRunEntity, UUID> {
    long countByStatus(WorkflowStatus status);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WorkflowRunEntity w where w.id = :workflowId")
    Optional<WorkflowRunEntity> findForUpdateById(@Param("workflowId") UUID workflowId);
}
