package com.agentic.sdlc.controlplane.governance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PolicyEvaluationRepository extends JpaRepository<PolicyEvaluationEntity, UUID> {
    List<PolicyEvaluationEntity> findByWorkflowIdOrderByCreatedAtAsc(UUID workflowId);
}
