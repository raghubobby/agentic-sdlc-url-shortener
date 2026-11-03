package com.agentic.sdlc.controlplane.approval;

import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.governance.RiskLevel;
import com.agentic.sdlc.controlplane.workflow.WorkflowNodeExecutionEntity;
import com.agentic.sdlc.controlplane.workflow.WorkflowNodeExecutionRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ApprovalServiceIdempotencyTest {
    private final ApprovalRequestRepository requestRepository = mock(ApprovalRequestRepository.class);
    private final ApprovalDecisionRepository decisionRepository = mock(ApprovalDecisionRepository.class);
    private final WorkflowNodeExecutionRepository nodeRepository = mock(WorkflowNodeExecutionRepository.class);
    private final AuditService auditService = mock(AuditService.class);

    private final ApprovalService service = new ApprovalService(
            requestRepository,
            decisionRepository,
            nodeRepository,
            auditService);

    @Test
    void identicalReviewerReplayIsIdempotentEvenAfterRequestIsResolved() {
        UUID workflowId = UUID.randomUUID();
        String nodeKey = "release-approval";
        WorkflowNodeExecutionEntity node = new WorkflowNodeExecutionEntity(UUID.randomUUID(), workflowId, nodeKey);
        ApprovalRequestEntity request = new ApprovalRequestEntity(
                workflowId, nodeKey, node.getGeneration(), RiskLevel.CRITICAL, 1, "release gate");
        request.approve();
        ApprovalDecisionEntity prior = new ApprovalDecisionEntity(
                request.getId(), "reviewer-a", ApprovalVote.APPROVE, "first click");

        when(nodeRepository.findByWorkflowIdAndNodeKey(workflowId, nodeKey)).thenReturn(Optional.of(node));
        when(requestRepository.findForUpdateByWorkflowIdAndNodeKeyAndNodeGeneration(
                workflowId, nodeKey, node.getGeneration())).thenReturn(Optional.of(request));
        when(decisionRepository.findByApprovalRequestIdAndActor(request.getId(), "reviewer-a"))
                .thenReturn(Optional.of(prior));

        ApprovalRequestEntity result = service.recordDecision(
                workflowId, nodeKey, ApprovalVote.APPROVE, "reviewer-a", "duplicate click");

        assertThat(result).isSameAs(request);
        verify(decisionRepository, never()).saveAndFlush(any());
        verifyNoInteractions(auditService);
    }
}
