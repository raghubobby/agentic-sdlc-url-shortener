package com.agentic.sdlc.controlplane.approval;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.governance.PolicyOutcome;
import com.agentic.sdlc.controlplane.workflow.NodeStatus;
import com.agentic.sdlc.controlplane.workflow.WorkflowNodeExecutionEntity;
import com.agentic.sdlc.controlplane.workflow.WorkflowNodeExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ApprovalService {
    private final ApprovalRequestRepository requestRepository;
    private final ApprovalDecisionRepository decisionRepository;
    private final WorkflowNodeExecutionRepository nodeRepository;
    private final AuditService auditService;

    public ApprovalService(
            ApprovalRequestRepository requestRepository,
            ApprovalDecisionRepository decisionRepository,
            WorkflowNodeExecutionRepository nodeRepository,
            AuditService auditService) {
        this.requestRepository = requestRepository;
        this.decisionRepository = decisionRepository;
        this.nodeRepository = nodeRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void ensureApprovalRequest(UUID workflowId, String nodeKey, PolicyOutcome outcome) {
        WorkflowNodeExecutionEntity node = nodeRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown node: " + nodeKey));
        ApprovalRequestEntity existing = requestRepository
                .findByWorkflowIdAndNodeKeyAndNodeGeneration(workflowId, nodeKey, node.getGeneration())
                .orElse(null);
        if (existing != null) return;
        if (node.getStatus() != NodeStatus.PENDING) return;

        ApprovalRequestEntity request = new ApprovalRequestEntity(
                workflowId, nodeKey, node.getGeneration(), outcome.risk().level(),
                outcome.requiredApprovals(), outcome.reason());
        requestRepository.save(request);
        node.markWaitingForApproval();
        node.setRequiresApproval(true);
        node.setRiskLevel(outcome.risk().level());
        auditService.append(workflowId, nodeKey, AuditEventType.APPROVAL_REQUESTED,
                "generation=%d;risk=%s;requiredApprovals=%d;reason=%s".formatted(
                        node.getGeneration(), outcome.risk().level(), outcome.requiredApprovals(), outcome.reason()));
    }

    @Transactional(readOnly = true)
    public boolean isApproved(UUID workflowId, String nodeKey) {
        WorkflowNodeExecutionEntity node = nodeRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown node: " + nodeKey));
        return requestRepository.findByWorkflowIdAndNodeKeyAndNodeGeneration(
                        workflowId, nodeKey, node.getGeneration())
                .map(request -> request.getStatus() == ApprovalStatus.APPROVED)
                .orElse(false);
    }

    @Transactional
    public ApprovalRequestEntity recordDecision(
            UUID workflowId, String nodeKey, ApprovalVote vote, String actor, String comment) {
        WorkflowNodeExecutionEntity node = nodeRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown node: " + nodeKey));
        ApprovalRequestEntity request = requestRepository
                .findForUpdateByWorkflowIdAndNodeKeyAndNodeGeneration(workflowId, nodeKey, node.getGeneration())
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found for current node generation"));

        var priorDecision = decisionRepository.findByApprovalRequestIdAndActor(request.getId(), actor);
        if (priorDecision.isPresent()) {
            if (priorDecision.get().getDecision() == vote) {
                // Idempotent replay: the same reviewer submitted the same decision again.
                return request;
            }
            throw new IllegalStateException("Actor has already voted differently on this approval request: " + actor);
        }
        if (request.getStatus() != ApprovalStatus.PENDING) {
            throw new IllegalStateException("Approval request is already resolved: " + request.getStatus());
        }

        decisionRepository.saveAndFlush(new ApprovalDecisionEntity(request.getId(), actor, vote, comment));
        auditService.append(workflowId, nodeKey, AuditEventType.APPROVAL_VOTE_RECORDED,
                "generation=%d;actor=%s;decision=%s".formatted(node.getGeneration(), actor, vote));

        if (vote == ApprovalVote.REJECT) {
            request.reject();
            node.markFailed(com.agentic.sdlc.controlplane.recovery.FailureCategory.POLICY,
                    "Human approval rejected by " + actor);
            auditService.append(workflowId, nodeKey, AuditEventType.APPROVAL_REJECTED,
                    "generation=%d;rejectedBy=%s".formatted(node.getGeneration(), actor));
            auditService.append(workflowId, nodeKey, AuditEventType.NODE_FAILED,
                    "failureCategory=POLICY;reason=human rejection");
            return request;
        }

        long approvals = decisionRepository.countByApprovalRequestIdAndDecision(request.getId(), ApprovalVote.APPROVE);
        if (approvals >= request.getRequiredApprovals()) {
            request.approve();
            node.markPendingAfterApproval();
            auditService.append(workflowId, nodeKey, AuditEventType.APPROVAL_QUORUM_REACHED,
                    "generation=%d;approvals=%d;required=%d".formatted(
                            node.getGeneration(), approvals, request.getRequiredApprovals()));
            auditService.append(workflowId, nodeKey, AuditEventType.APPROVAL_GRANTED,
                    "Approval policy satisfied for generation=" + node.getGeneration());
        }
        return request;
    }

    @Transactional
    public void supersedePendingForReplan(UUID workflowId, Set<String> invalidatedNodeKeys, int planRevision) {
        requestRepository.findByWorkflowIdAndStatus(workflowId, ApprovalStatus.PENDING).stream()
                .filter(request -> invalidatedNodeKeys.contains(request.getNodeKey()))
                .forEach(request -> {
                    request.supersede();
                    auditService.append(workflowId, request.getNodeKey(), AuditEventType.APPROVAL_SUPERSEDED,
                            "approvalId=%s;nodeGeneration=%d;planRevision=%d".formatted(
                                    request.getId(), request.getNodeGeneration(), planRevision));
                });
    }

    @Transactional(readOnly = true)
    public ApprovalView view(UUID workflowId, String nodeKey) {
        WorkflowNodeExecutionEntity node = nodeRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown node: " + nodeKey));
        ApprovalRequestEntity request = requestRepository
                .findByWorkflowIdAndNodeKeyAndNodeGeneration(workflowId, nodeKey, node.getGeneration())
                .orElseThrow(() -> new IllegalArgumentException("Approval request not found for current node generation"));
        List<ApprovalDecisionEntity> decisions = decisionRepository.findByApprovalRequestIdOrderByDecidedAtAsc(request.getId());
        return ApprovalView.from(request, decisions);
    }

    public record ApprovalView(
            UUID id, UUID workflowId, String nodeKey, int nodeGeneration, ApprovalStatus status,
            com.agentic.sdlc.controlplane.governance.RiskLevel riskLevel, int requiredApprovals,
            String reason, List<DecisionView> decisions
    ) {
        static ApprovalView from(ApprovalRequestEntity request, List<ApprovalDecisionEntity> decisions) {
            return new ApprovalView(
                    request.getId(), request.getWorkflowId(), request.getNodeKey(), request.getNodeGeneration(),
                    request.getStatus(), request.getRiskLevel(), request.getRequiredApprovals(), request.getReason(),
                    decisions.stream().map(DecisionView::from).toList());
        }
    }

    public record DecisionView(String actor, ApprovalVote decision, String comment, java.time.Instant decidedAt) {
        static DecisionView from(ApprovalDecisionEntity decision) {
            return new DecisionView(decision.getActor(), decision.getDecision(), decision.getComment(), decision.getDecidedAt());
        }
    }
}
