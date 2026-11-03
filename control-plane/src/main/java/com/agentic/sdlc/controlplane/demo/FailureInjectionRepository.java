package com.agentic.sdlc.controlplane.demo;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FailureInjectionRepository extends JpaRepository<FailureInjectionEntity, UUID> {
    List<FailureInjectionEntity> findByWorkflowIdOrderByCreatedAtAsc(UUID workflowId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FailureInjectionEntity f where f.workflowId = :workflowId and f.nodeKey = :nodeKey " +
            "and f.active = true order by f.createdAt asc")
    List<FailureInjectionEntity> findActiveForUpdate(
            @Param("workflowId") UUID workflowId,
            @Param("nodeKey") String nodeKey);
}
