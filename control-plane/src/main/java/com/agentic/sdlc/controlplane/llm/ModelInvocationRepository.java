package com.agentic.sdlc.controlplane.llm;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModelInvocationRepository extends JpaRepository<ModelInvocationEntity, UUID> {
    List<ModelInvocationEntity> findByWorkflowIdOrderByCreatedAtAsc(UUID workflowId);
}
