package com.agentic.sdlc.controlplane.demo;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import com.agentic.sdlc.controlplane.workflow.WorkflowDefinition;
import com.agentic.sdlc.controlplane.workflow.WorkflowRunRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class FailureInjectionService {
    private static final Set<FailureCategory> ALLOWED = EnumSet.of(
            FailureCategory.TRANSIENT_INFRASTRUCTURE,
            FailureCategory.RATE_LIMIT,
            FailureCategory.VALIDATION);

    private final FailureInjectionRepository repository;
    private final WorkflowRunRepository workflowRepository;
    private final AuditService auditService;
    private final boolean enabled;
    private final WorkflowDefinition definition = WorkflowDefinition.defaultEngineeringWorkflow();

    public FailureInjectionService(
            FailureInjectionRepository repository,
            WorkflowRunRepository workflowRepository,
            AuditService auditService,
            @Value("${agentic.demo.failure-injection.enabled:false}") boolean enabled) {
        this.repository = repository;
        this.workflowRepository = workflowRepository;
        this.auditService = auditService;
        this.enabled = enabled;
    }

    public boolean enabled() { return enabled; }

    @Transactional
    public FailureInjectionView arm(UUID workflowId, String nodeKey, FailureCategory category,
                                    int failures, boolean sideEffectsPossible, String message, String actor) {
        assertEnabled();
        workflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow: " + workflowId));
        definition.node(nodeKey);
        if (!ALLOWED.contains(category)) {
            throw new IllegalArgumentException("Failure category is not allowed for demo injection: " + category);
        }
        if (failures < 1 || failures > 5) {
            throw new IllegalArgumentException("failures must be between 1 and 5");
        }
        FailureInjectionEntity entity = repository.save(new FailureInjectionEntity(
                workflowId, nodeKey, category, failures, sideEffectsPossible,
                message == null || message.isBlank() ? "Controlled failure injection" : message,
                actor == null || actor.isBlank() ? "demo-operator" : actor));
        auditService.append(workflowId, nodeKey, AuditEventType.FAILURE_INJECTION_ARMED,
                "injectionId=%s;category=%s;failures=%d;sideEffectsPossible=%s".formatted(
                        entity.getId(), category, failures, sideEffectsPossible));
        return FailureInjectionView.from(entity);
    }

    @Transactional
    public Optional<InjectedFailure> consume(UUID workflowId, String nodeKey) {
        if (!enabled) return Optional.empty();
        List<FailureInjectionEntity> active = repository.findActiveForUpdate(workflowId, nodeKey);
        if (active.isEmpty()) return Optional.empty();
        FailureInjectionEntity entity = active.getFirst();
        entity.consume();
        auditService.append(workflowId, nodeKey, AuditEventType.FAILURE_INJECTED,
                "injectionId=%s;category=%s;remaining=%d".formatted(
                        entity.getId(), entity.getFailureCategory(), entity.getRemainingFailures()));
        return Optional.of(new InjectedFailure(
                entity.getFailureCategory(), entity.getMessage(), entity.isSideEffectsPossible()));
    }

    @Transactional
    public FailureInjectionView clear(UUID workflowId, UUID injectionId) {
        assertEnabled();
        FailureInjectionEntity entity = repository.findById(injectionId)
                .filter(f -> f.getWorkflowId().equals(workflowId))
                .orElseThrow(() -> new IllegalArgumentException("Unknown failure injection: " + injectionId));
        entity.clear();
        auditService.append(workflowId, entity.getNodeKey(), AuditEventType.FAILURE_INJECTION_CLEARED,
                "injectionId=" + injectionId);
        return FailureInjectionView.from(entity);
    }

    @Transactional(readOnly = true)
    public List<FailureInjectionView> list(UUID workflowId) {
        assertEnabled();
        return repository.findByWorkflowIdOrderByCreatedAtAsc(workflowId).stream()
                .map(FailureInjectionView::from).toList();
    }

    private void assertEnabled() {
        if (!enabled) throw new IllegalStateException("Failure injection is disabled. Set AGENTIC_FAILURE_INJECTION_ENABLED=true for demo use.");
    }

    public record FailureInjectionView(
            UUID id, UUID workflowId, String nodeKey, FailureCategory category,
            int remainingFailures, boolean sideEffectsPossible, boolean active,
            String message, String createdBy, java.time.Instant createdAt) {
        static FailureInjectionView from(FailureInjectionEntity e) {
            return new FailureInjectionView(e.getId(), e.getWorkflowId(), e.getNodeKey(), e.getFailureCategory(),
                    e.getRemainingFailures(), e.isSideEffectsPossible(), e.isActive(), e.getMessage(),
                    e.getCreatedBy(), e.getCreatedAt());
        }
    }
}
