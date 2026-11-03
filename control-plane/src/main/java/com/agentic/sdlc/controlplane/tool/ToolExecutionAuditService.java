package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ToolExecutionAuditService {
    private final ToolExecutionRepository repository;
    private final AuditService auditService;

    public ToolExecutionAuditService(ToolExecutionRepository repository, AuditService auditService) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public UUID started(ToolExecutionContext context, ToolAction action, AgentCapability capability) {
        UUID id = UUID.randomUUID();
        repository.save(new ToolExecutionEntity(id, context, action, capability, ToolHashing.actionHash(action)));
        auditService.append(context.workflowId(), context.nodeKey(), AuditEventType.TOOL_EXECUTION_STARTED,
                "tool=%s;requiredCapability=%s;executionId=%s".formatted(action.tool(), capability, id));
        return id;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void succeeded(UUID id, ToolResult result, long latencyMs) {
        ToolExecutionEntity entity = repository.findById(id).orElseThrow();
        entity.succeed(result, ToolHashing.sha256(result.output()), latencyMs);
        auditService.append(entity.getWorkflowId(), entity.getNodeKey(), AuditEventType.TOOL_EXECUTION_SUCCEEDED,
                "tool=%s;executionId=%s;exitCode=%d;latencyMs=%d".formatted(
                        entity.getToolType(), id, result.exitCode(), latencyMs));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failed(UUID id, ToolResult result, long latencyMs) {
        ToolExecutionEntity entity = repository.findById(id).orElseThrow();
        entity.fail(result, ToolHashing.sha256(result.output()), latencyMs);
        auditService.append(entity.getWorkflowId(), entity.getNodeKey(), AuditEventType.TOOL_EXECUTION_FAILED,
                "tool=%s;executionId=%s;exitCode=%d;latencyMs=%d;summary=%s".formatted(
                        entity.getToolType(), id, result.exitCode(), latencyMs, result.summary()));
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void denied(UUID id, String reason, long latencyMs) {
        ToolExecutionEntity entity = repository.findById(id).orElseThrow();
        entity.deny(reason, latencyMs);
        auditService.append(entity.getWorkflowId(), entity.getNodeKey(), AuditEventType.TOOL_EXECUTION_DENIED,
                "tool=%s;executionId=%s;latencyMs=%d;reason=%s".formatted(
                        entity.getToolType(), id, latencyMs, reason));
    }

    @Transactional(readOnly = true)
    public List<ToolExecutionView> workflow(UUID workflowId) {
        return repository.findByWorkflowIdOrderByStartedAtAsc(workflowId).stream()
                .map(ToolExecutionView::from).toList();
    }
}
