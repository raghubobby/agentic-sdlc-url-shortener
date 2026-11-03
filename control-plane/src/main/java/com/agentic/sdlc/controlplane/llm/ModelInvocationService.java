package com.agentic.sdlc.controlplane.llm;

import com.agentic.sdlc.controlplane.artifact.ArtifactHasher;
import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ModelInvocationService {
    private final ModelInvocationRepository repository;
    private final ArtifactHasher hasher;
    private final AuditService auditService;

    public ModelInvocationService(ModelInvocationRepository repository, ArtifactHasher hasher, AuditService auditService) {
        this.repository = repository;
        this.hasher = hasher;
        this.auditService = auditService;
    }

    @Transactional
    public void record(ModelRequest request, ModelResponse response) {
        ModelInvocationStatus status = response.refused()
                ? ModelInvocationStatus.REFUSED
                : ModelInvocationStatus.SUCCEEDED;
        String responseHash = response.structuredOutput() == null
                ? null
                : hasher.sha256(response.structuredOutput().toString());
        repository.save(new ModelInvocationEntity(
                request.requestId(), request.workflowId(), request.nodeKey(), request.agentRole(),
                response.provider(), response.model(), hasher.sha256(request.prompt().render()), responseHash,
                status, response.refusalReason(), null, response.latencyMs(), response.inputTokens(), response.outputTokens()));
        auditService.append(request.workflowId(), request.nodeKey(), AuditEventType.MODEL_INVOCATION_COMPLETED,
                "requestId=%s;agent=%s;provider=%s;model=%s;status=%s;latencyMs=%d".formatted(
                        request.requestId(), request.agentRole(), response.provider(), response.model(), status, response.latencyMs()));
    }

    @Transactional
    public void recordFailure(ModelRequest request, String provider, String model, long latencyMs, String reason) {
        repository.save(new ModelInvocationEntity(
                request.requestId(), request.workflowId(), request.nodeKey(), request.agentRole(),
                provider, model, hasher.sha256(request.prompt().render()), null,
                ModelInvocationStatus.FAILED, null, reason, latencyMs, null, null));
        auditService.append(request.workflowId(), request.nodeKey(), AuditEventType.MODEL_INVOCATION_FAILED,
                "requestId=%s;agent=%s;provider=%s;model=%s;reason=%s".formatted(
                        request.requestId(), request.agentRole(), provider, model, reason));
    }

    @Transactional(readOnly = true)
    public List<ModelInvocationEntity> history(UUID workflowId) {
        return repository.findByWorkflowIdOrderByCreatedAtAsc(workflowId);
    }
}
