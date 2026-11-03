package com.agentic.sdlc.controlplane.workflow;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class WorkflowStateService {
    private final WorkflowRunRepository runRepository;
    private final AuditService auditService;

    public WorkflowStateService(WorkflowRunRepository runRepository, AuditService auditService) {
        this.runRepository = runRepository;
        this.auditService = auditService;
    }

    @Transactional
    public void markRunning(UUID workflowId) {
        WorkflowRunEntity run = runRepository.findById(workflowId).orElseThrow();
        if (run.getStatus() == WorkflowStatus.CREATED) {
            run.markRunning();
            auditService.append(workflowId, null, AuditEventType.WORKFLOW_STARTED,
                    "Workflow scheduler started");
        } else if (run.getStatus() == WorkflowStatus.WAITING_FOR_APPROVAL
                || run.getStatus() == WorkflowStatus.WAITING_FOR_CLARIFICATION) {
            WorkflowStatus prior = run.getStatus();
            run.markRunning();
            auditService.append(workflowId, null, AuditEventType.WORKFLOW_RESUMED,
                    "Workflow resumed after " + prior.name().toLowerCase());
        }
    }

    @Transactional
    public void markWaitingForApproval(UUID workflowId) {
        WorkflowRunEntity run = runRepository.findById(workflowId).orElseThrow();
        if (run.getStatus() != WorkflowStatus.WAITING_FOR_APPROVAL) {
            run.markWaitingForApproval();
            auditService.append(workflowId, null, AuditEventType.WORKFLOW_WAITING_FOR_APPROVAL,
                    "Workflow paused until human approval policy is satisfied");
        }
    }

    @Transactional
    public void markWaitingForClarification(UUID workflowId) {
        WorkflowRunEntity run = runRepository.findById(workflowId).orElseThrow();
        if (run.getStatus() != WorkflowStatus.WAITING_FOR_CLARIFICATION) {
            run.markWaitingForClarification();
            auditService.append(workflowId, null, AuditEventType.WORKFLOW_WAITING_FOR_CLARIFICATION,
                    "Workflow safe-stopped until material requirement ambiguity is resolved by a human");
        }
    }

    @Transactional
    public void markCompleted(UUID workflowId) {
        WorkflowRunEntity run = runRepository.findById(workflowId).orElseThrow();
        if (run.getStatus() != WorkflowStatus.COMPLETED) {
            run.markCompleted();
            auditService.append(workflowId, null, AuditEventType.WORKFLOW_COMPLETED,
                    "All workflow nodes completed successfully");
        }
    }

    @Transactional
    public void markFailed(UUID workflowId) {
        WorkflowRunEntity run = runRepository.findById(workflowId).orElseThrow();
        if (run.getStatus() != WorkflowStatus.FAILED) {
            run.markFailed();
            auditService.append(workflowId, null, AuditEventType.WORKFLOW_FAILED,
                    "At least one workflow node failed or was denied");
        }
    }
}
