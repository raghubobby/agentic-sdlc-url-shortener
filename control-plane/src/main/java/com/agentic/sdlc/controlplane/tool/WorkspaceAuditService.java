package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.util.UUID;

@Service
public class WorkspaceAuditService {
    private final AuditService auditService;

    public WorkspaceAuditService(AuditService auditService) { this.auditService = auditService; }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void created(UUID workflowId, String nodeKey, Path workspace, String baseCommit) {
        auditService.append(workflowId, nodeKey, AuditEventType.WORKSPACE_CREATED,
                "workspace=" + workspace + ";baseCommit=" + baseCommit);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void assembled(UUID workflowId, String nodeKey, int patches) {
        auditService.append(workflowId, nodeKey, AuditEventType.WORKSPACE_ASSEMBLED,
                "upstreamPatches=" + patches);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void discarded(UUID workflowId, String nodeKey, Path workspace) {
        auditService.append(workflowId, nodeKey, AuditEventType.WORKSPACE_DISCARDED,
                "workspace=" + workspace);
    }
}
