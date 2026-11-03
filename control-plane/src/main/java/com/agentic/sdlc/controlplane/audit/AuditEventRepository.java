package com.agentic.sdlc.controlplane.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEventEntity, Long> {
    List<AuditEventEntity> findByWorkflowIdOrderByEventSequence(UUID workflowId);
    long countByEventType(AuditEventType eventType);
}
