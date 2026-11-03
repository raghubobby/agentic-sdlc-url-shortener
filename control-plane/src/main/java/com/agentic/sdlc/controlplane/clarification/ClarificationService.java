package com.agentic.sdlc.controlplane.clarification;

import com.agentic.sdlc.controlplane.artifact.ArtifactRegistryService;
import com.agentic.sdlc.controlplane.artifact.ArtifactView;
import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.replan.ReplanService;
import com.agentic.sdlc.controlplane.replan.ReplanView;
import com.agentic.sdlc.controlplane.workflow.WorkflowRunEntity;
import com.agentic.sdlc.controlplane.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ClarificationService {
    private final ClarificationRequestRepository repository;
    private final WorkflowRunRepository runRepository;
    private final ArtifactRegistryService artifactRegistry;
    private final ReplanService replanService;
    private final AuditService auditService;

    public ClarificationService(
            ClarificationRequestRepository repository,
            WorkflowRunRepository runRepository,
            ArtifactRegistryService artifactRegistry,
            ReplanService replanService,
            AuditService auditService) {
        this.repository = repository;
        this.runRepository = runRepository;
        this.artifactRegistry = artifactRegistry;
        this.replanService = replanService;
        this.auditService = auditService;
    }

    @Transactional
    public ClarificationView request(UUID workflowId, String nodeKey, List<String> questions) {
        if (questions == null || questions.isEmpty()) throw new IllegalArgumentException("questions are required");
        WorkflowRunEntity run = runRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow: " + workflowId));
        ClarificationRequestEntity existing = repository
                .findByWorkflowIdAndPlanRevisionAndNodeKey(workflowId, run.getPlanRevision(), nodeKey)
                .orElse(null);
        if (existing != null) return ClarificationView.from(existing);

        ClarificationRequestEntity entity = new ClarificationRequestEntity(
                workflowId, run.getPlanRevision(), nodeKey, ClarificationCodec.encode(questions));
        repository.save(entity);
        auditService.append(workflowId, nodeKey, AuditEventType.CLARIFICATION_REQUESTED,
                "clarificationId=%s;planRevision=%d;questionCount=%d".formatted(
                        entity.getId(), run.getPlanRevision(), questions.size()));
        return ClarificationView.from(entity);
    }

    @Transactional(readOnly = true)
    public boolean hasOpen(UUID workflowId) {
        return repository.existsByWorkflowIdAndStatus(workflowId, ClarificationStatus.OPEN);
    }

    @Transactional(readOnly = true)
    public List<ClarificationView> history(UUID workflowId) {
        return repository.findByWorkflowIdOrderByRequestedAtAsc(workflowId).stream()
                .map(ClarificationView::from).toList();
    }

    @Transactional
    public Resolution resolve(UUID workflowId, UUID clarificationId, String answer, String resolvedBy) {
        if (answer == null || answer.isBlank()) throw new IllegalArgumentException("answer is required");
        if (resolvedBy == null || resolvedBy.isBlank()) throw new IllegalArgumentException("resolvedBy is required");
        ClarificationRequestEntity request = repository.findForUpdateById(clarificationId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown clarification: " + clarificationId));
        if (!request.getWorkflowId().equals(workflowId)) {
            throw new IllegalArgumentException("Clarification does not belong to workflow: " + workflowId);
        }
        request.resolve(answer.trim(), resolvedBy.trim());
        auditService.append(workflowId, request.getNodeKey(), AuditEventType.CLARIFICATION_RESOLVED,
                "clarificationId=%s;resolvedBy=%s".formatted(clarificationId, resolvedBy.trim()));

        WorkflowRunEntity run = runRepository.findById(workflowId).orElseThrow();
        var currentEntity = artifactRegistry.activeArtifact(workflowId, request.getNodeKey());
        ArtifactView current = new ArtifactView(
                currentEntity.getId(), currentEntity.getWorkflowId(), currentEntity.getNodeKey(), currentEntity.getType(),
                currentEntity.getVersion(), currentEntity.getStatus(), currentEntity.getContentHash(), currentEntity.getContent(),
                currentEntity.getCreatedBy(), currentEntity.getCreatedAt());
        String clarifiedRequirement = run.getRequirement()
                + "\n\nHuman clarification (authoritative):\n" + answer.trim();
        String revisedArtifact = current.content()
                + "\n\n--- HUMAN CLARIFICATION ---\n"
                + "resolvedBy=" + resolvedBy.trim() + "\n"
                + answer.trim() + "\n";
        ReplanView replan = replanService.revise(
                workflowId,
                "clarification-" + clarificationId,
                request.getNodeKey(),
                revisedArtifact,
                clarifiedRequirement,
                "Human clarification resolved material requirement ambiguity",
                resolvedBy.trim());
        return new Resolution(ClarificationView.from(request), replan);
    }

    public record Resolution(ClarificationView clarification, ReplanView replan) {}
}
