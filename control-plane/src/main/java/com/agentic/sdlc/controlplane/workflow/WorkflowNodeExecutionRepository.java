package com.agentic.sdlc.controlplane.workflow;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WorkflowNodeExecutionRepository extends JpaRepository<WorkflowNodeExecutionEntity, UUID> {
    List<WorkflowNodeExecutionEntity> findByWorkflowIdOrderByNodeKey(UUID workflowId);
    Optional<WorkflowNodeExecutionEntity> findByWorkflowIdAndNodeKey(UUID workflowId, String nodeKey);
}
