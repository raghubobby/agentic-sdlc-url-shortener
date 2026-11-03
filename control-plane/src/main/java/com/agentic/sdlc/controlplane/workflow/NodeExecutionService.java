package com.agentic.sdlc.controlplane.workflow;

import com.agentic.sdlc.controlplane.approval.ApprovalService;
import com.agentic.sdlc.controlplane.demo.FailureInjectionService;
import com.agentic.sdlc.controlplane.execution.CompensationResult;
import com.agentic.sdlc.controlplane.execution.NodeExecutionContext;
import com.agentic.sdlc.controlplane.execution.NodeWorkExecutor;
import com.agentic.sdlc.controlplane.execution.NodeWorkResult;
import com.agentic.sdlc.controlplane.governance.GovernanceService;
import com.agentic.sdlc.controlplane.governance.PolicyDecision;
import com.agentic.sdlc.controlplane.governance.PolicyOutcome;
import com.agentic.sdlc.controlplane.recovery.RetryEvaluator;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class NodeExecutionService {
    private final GovernanceService governanceService;
    private final ApprovalService approvalService;
    private final NodeStateService nodeStateService;
    private final RetryEvaluator retryEvaluator;
    private final NodeWorkExecutor nodeWorkExecutor;
    private final FailureInjectionService failureInjectionService;

    public NodeExecutionService(
            GovernanceService governanceService,
            ApprovalService approvalService,
            NodeStateService nodeStateService,
            RetryEvaluator retryEvaluator,
            NodeWorkExecutor nodeWorkExecutor,
            FailureInjectionService failureInjectionService) {
        this.governanceService = governanceService;
        this.approvalService = approvalService;
        this.nodeStateService = nodeStateService;
        this.retryEvaluator = retryEvaluator;
        this.nodeWorkExecutor = nodeWorkExecutor;
        this.failureInjectionService = failureInjectionService;
    }

    public void execute(UUID workflowId, WorkflowNodeDefinition definition) {
        PolicyOutcome policy = governanceService.evaluate(workflowId, definition);
        nodeStateService.recordRisk(workflowId, definition.key(), policy);

        if (policy.decision() == PolicyDecision.DENY) {
            nodeStateService.markPolicyDenied(workflowId, definition.key(), policy.reason());
            return;
        }

        if (policy.decision() == PolicyDecision.REQUIRE_HUMAN_APPROVAL
                && !approvalService.isApproved(workflowId, definition.key())) {
            approvalService.ensureApprovalRequest(workflowId, definition.key(), policy);
            return;
        }

        while (true) {
            int attempt = nodeStateService.startAttempt(workflowId, definition.key());
            NodeExecutionContext context = new NodeExecutionContext(workflowId, definition, attempt);
            NodeWorkResult result;
            try {
                var injected = failureInjectionService.consume(workflowId, definition.key());
                result = injected
                        .map(failure -> NodeWorkResult.failure(
                                failure.category(), failure.message(), failure.sideEffectsPossible()))
                        .orElseGet(() -> nodeWorkExecutor.execute(context));
            } catch (RuntimeException ex) {
                result = NodeWorkResult.failure(
                        com.agentic.sdlc.controlplane.recovery.FailureCategory.UNKNOWN,
                        ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), true);
            }

            if (result.success()) {
                nodeStateService.markSucceeded(workflowId, definition, result, result.createdBy());
                return;
            }

            if (retryEvaluator.shouldRetry(definition.retryPolicy(), attempt, result.failureCategory())) {
                nodeStateService.markRetryPending(
                        workflowId, definition.key(), result.failureCategory(), result.message());
                continue;
            }

            nodeStateService.markFailed(workflowId, definition.key(), result.failureCategory(), result.message());
            if (definition.compensatable() && result.sideEffectsPossible()) {
                compensate(context, result);
            }
            return;
        }
    }

    private void compensate(NodeExecutionContext context, NodeWorkResult failedResult) {
        nodeStateService.markCompensationRunning(context.workflowId(), context.definition().key());
        try {
            CompensationResult result = nodeWorkExecutor.compensate(context, failedResult);
            if (result.success()) {
                nodeStateService.markCompensationSucceeded(
                        context.workflowId(), context.definition().key(), result.message());
            } else {
                nodeStateService.markCompensationFailed(
                        context.workflowId(), context.definition().key(), result.message());
            }
        } catch (RuntimeException ex) {
            nodeStateService.markCompensationFailed(
                    context.workflowId(), context.definition().key(),
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
        }
    }
}
