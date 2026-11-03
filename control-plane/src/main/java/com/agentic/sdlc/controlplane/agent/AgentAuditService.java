package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AgentAuditService {
    private final AuditService auditService;

    public AgentAuditService(AuditService auditService) {
        this.auditService = auditService;
    }

    @Transactional
    public void started(UUID workflowId, String nodeKey, AgentRole role, CapabilityEnvelope envelope) {
        auditService.append(workflowId, nodeKey, AuditEventType.AGENT_STARTED,
                "role=%s;allowed=%s;denied=%s".formatted(role, envelope.allowed(), envelope.denied()));
    }

    @Transactional
    public void completed(UUID workflowId, String nodeKey, AgentResult result) {
        auditService.append(workflowId, nodeKey, AuditEventType.AGENT_COMPLETED,
                "role=%s;provider=%s;model=%s;confidence=%.3f".formatted(
                        result.role(), result.provider(), result.model(), result.output().confidence()));
    }

    @Transactional
    public void capabilityDenied(UUID workflowId, String nodeKey, AgentRole role, CapabilityViolationException ex) {
        auditService.append(workflowId, nodeKey, AuditEventType.AGENT_CAPABILITY_DENIED,
                "role=%s;violations=%s".formatted(role, ex.violations()));
    }

    @Transactional
    public void refused(UUID workflowId, String nodeKey, AgentRole role, String reason) {
        auditService.append(workflowId, nodeKey, AuditEventType.AGENT_REFUSED,
                "role=%s;reason=%s".formatted(role, reason));
    }
}
