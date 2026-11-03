package com.agentic.sdlc.controlplane.workflow;

import com.agentic.sdlc.controlplane.artifact.ArtifactRegistryService;
import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.execution.NodeWorkResult;
import com.agentic.sdlc.controlplane.governance.PolicyOutcome;
import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NodeStateService {
    private final WorkflowNodeExecutionRepository nodeRepository;
    private final WorkflowRunRepository runRepository;
    private final ArtifactRegistryService artifactRegistryService;
    private final AuditService auditService;

    public NodeStateService(
            WorkflowNodeExecutionRepository nodeRepository,
            WorkflowRunRepository runRepository,
            ArtifactRegistryService artifactRegistryService,
            AuditService auditService) {
        this.nodeRepository = nodeRepository;
        this.runRepository = runRepository;
        this.artifactRegistryService = artifactRegistryService;
        this.auditService = auditService;
    }

    @Transactional
    public void recordRisk(UUID workflowId, String nodeKey, PolicyOutcome outcome) {
        WorkflowNodeExecutionEntity node = node(workflowId, nodeKey);
        node.setRiskLevel(outcome.risk().level());
        node.setRequiresApproval(outcome.requiredApprovals() > 0);
    }

    @Transactional
    public int startAttempt(UUID workflowId, String nodeKey) {
        WorkflowNodeExecutionEntity node = node(workflowId, nodeKey);
        if (node.getStatus() != NodeStatus.PENDING) {
            throw new IllegalStateException("Node is not ready to start: " + nodeKey + " status=" + node.getStatus());
        }
        node.markRunning();
        auditService.append(workflowId, nodeKey, AuditEventType.NODE_STARTED,
                "attempt=" + node.getAttempt());
        return node.getAttempt();
    }

    @Transactional
    public void markSucceeded(
            UUID workflowId, WorkflowNodeDefinition definition, NodeWorkResult result, String createdBy) {
        WorkflowNodeExecutionEntity node = node(workflowId, definition.key());
        if (node.getStatus() != NodeStatus.RUNNING) {
            throw new IllegalStateException("Node is not running: " + definition.key());
        }
        artifactRegistryService.publish(
                workflowId, definition.key(), definition.dependencies(), result.artifactType(),
                result.artifactContent(), createdBy);
        int planRevision = runRepository.findById(workflowId).orElseThrow().getPlanRevision();
        node.markSucceeded(planRevision);
        auditService.append(workflowId, definition.key(), AuditEventType.NODE_SUCCEEDED,
                "attempt=%d;generation=%d;planRevision=%d".formatted(
                        node.getAttempt(), node.getGeneration(), planRevision));
    }

    @Transactional
    public void markRetryPending(UUID workflowId, String nodeKey, FailureCategory category, String error) {
        WorkflowNodeExecutionEntity node = node(workflowId, nodeKey);
        node.markRetryPending(category, error);
        auditService.append(workflowId, nodeKey, AuditEventType.NODE_RETRY_SCHEDULED,
                "attempt=%d;category=%s;reason=%s".formatted(node.getAttempt(), category, error));
    }

    @Transactional
    public void markFailed(UUID workflowId, String nodeKey, FailureCategory category, String error) {
        WorkflowNodeExecutionEntity node = node(workflowId, nodeKey);
        node.markFailed(category, error);
        auditService.append(workflowId, nodeKey, AuditEventType.NODE_FAILED,
                "attempt=%d;category=%s;reason=%s".formatted(node.getAttempt(), category, error));
    }

    @Transactional
    public void markPolicyDenied(UUID workflowId, String nodeKey, String reason) {
        WorkflowNodeExecutionEntity node = node(workflowId, nodeKey);
        node.markFailed(FailureCategory.POLICY, reason);
        auditService.append(workflowId, nodeKey, AuditEventType.POLICY_DENIED, reason);
        auditService.append(workflowId, nodeKey, AuditEventType.NODE_FAILED,
                "category=POLICY;reason=" + reason);
    }

    @Transactional
    public void markCompensationRunning(UUID workflowId, String nodeKey) {
        WorkflowNodeExecutionEntity node = node(workflowId, nodeKey);
        node.markCompensationRunning();
        auditService.append(workflowId, nodeKey, AuditEventType.COMPENSATION_STARTED,
                "Compensation started");
    }

    @Transactional
    public void markCompensationSucceeded(UUID workflowId, String nodeKey, String message) {
        WorkflowNodeExecutionEntity node = node(workflowId, nodeKey);
        node.markCompensationSucceeded();
        auditService.append(workflowId, nodeKey, AuditEventType.COMPENSATION_SUCCEEDED, message);
    }

    @Transactional
    public void markCompensationFailed(UUID workflowId, String nodeKey, String message) {
        WorkflowNodeExecutionEntity node = node(workflowId, nodeKey);
        node.markCompensationFailed();
        auditService.append(workflowId, nodeKey, AuditEventType.COMPENSATION_FAILED, message);
    }

    private WorkflowNodeExecutionEntity node(UUID workflowId, String nodeKey) {
        return nodeRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown node: " + nodeKey));
    }
}
