package com.agentic.sdlc.controlplane.replan;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReplanRecordRepository extends JpaRepository<ReplanRecordEntity, UUID> {
    Optional<ReplanRecordEntity> findByWorkflowIdAndChangeId(UUID workflowId, String changeId);
    List<ReplanRecordEntity> findByWorkflowIdOrderByCreatedAtAsc(UUID workflowId);
}
