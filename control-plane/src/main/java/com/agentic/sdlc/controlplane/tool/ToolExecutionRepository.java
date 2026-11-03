package com.agentic.sdlc.controlplane.tool;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ToolExecutionRepository extends JpaRepository<ToolExecutionEntity, UUID> {
    List<ToolExecutionEntity> findByWorkflowIdOrderByStartedAtAsc(UUID workflowId);
}
