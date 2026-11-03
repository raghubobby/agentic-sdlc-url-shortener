package com.agentic.sdlc.controlplane.workflow;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.clarification.ClarificationService;
import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.tool.WorkspaceManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class WorkflowEngine implements AutoCloseable {
    private final WorkflowRunRepository runRepository;
    private final WorkflowNodeExecutionRepository nodeRepository;
    private final NodeExecutionService nodeExecutionService;
    private final AuditService auditService;
    private final WorkflowStateService stateService;
    private final WorkspaceManager workspaceManager;
    private final ClarificationService clarificationService;
    private final WorkflowDefinition definition = WorkflowDefinition.defaultEngineeringWorkflow();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public WorkflowEngine(
            WorkflowRunRepository runRepository,
            WorkflowNodeExecutionRepository nodeRepository,
            NodeExecutionService nodeExecutionService,
            AuditService auditService,
            WorkflowStateService stateService,
            WorkspaceManager workspaceManager,
            ClarificationService clarificationService) {
        this.runRepository = runRepository;
        this.nodeRepository = nodeRepository;
        this.nodeExecutionService = nodeExecutionService;
        this.auditService = auditService;
        this.stateService = stateService;
        this.workspaceManager = workspaceManager;
        this.clarificationService = clarificationService;
    }

    @Transactional
    public UUID create(String scenario, String requirement) {
        UUID workflowId = UUID.randomUUID();
        String baseCommit = workspaceManager.currentHead();
        WorkflowRunEntity run = new WorkflowRunEntity(workflowId, scenario, requirement, baseCommit);
        runRepository.save(run);
        definition.nodes().forEach(node -> nodeRepository.save(
                new WorkflowNodeExecutionEntity(UUID.randomUUID(), workflowId, node.key())));
        auditService.append(workflowId, null, AuditEventType.WORKFLOW_CREATED,
                "scenario=" + scenario + ";baseCommit=" + baseCommit);
        return workflowId;
    }

    public void runUntilBlocked(UUID workflowId) {
        stateService.markRunning(workflowId);
        while (true) {
            if (clarificationService.hasOpen(workflowId)) {
                stateService.markWaitingForClarification(workflowId);
                return;
            }
            Map<String, NodeStatus> statuses = currentStatuses(workflowId);
            if (statuses.values().stream().anyMatch(status -> status == NodeStatus.FAILED)) {
                stateService.markFailed(workflowId);
                return;
            }
            if (statuses.values().stream().allMatch(status -> status == NodeStatus.SUCCEEDED)) {
                stateService.markCompleted(workflowId);
                return;
            }
            if (statuses.values().stream().anyMatch(status -> status == NodeStatus.WAITING_FOR_APPROVAL)) {
                stateService.markWaitingForApproval(workflowId);
                return;
            }

            List<WorkflowNodeDefinition> ready = definition.readyNodes(statuses);
            if (ready.isEmpty()) {
                throw new IllegalStateException("Workflow is blocked with no ready nodes and no approval gate");
            }

            List<CompletableFuture<Void>> futures = ready.stream()
                    .map(node -> CompletableFuture.runAsync(
                            () -> nodeExecutionService.execute(workflowId, node), executor))
                    .toList();
            try {
                CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
            } catch (CompletionException ex) {
                stateService.markFailed(workflowId);
                throw new IllegalStateException("Workflow execution stopped after an unexpected node failure", ex.getCause());
            }
        }
    }

    @Transactional(readOnly = true)
    public WorkflowSnapshot snapshot(UUID workflowId) {
        WorkflowRunEntity run = runRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow: " + workflowId));
        List<WorkflowNodeExecutionEntity> nodes = nodeRepository.findByWorkflowIdOrderByNodeKey(workflowId);
        return WorkflowSnapshot.from(run, nodes);
    }

    private Map<String, NodeStatus> currentStatuses(UUID workflowId) {
        return nodeRepository.findByWorkflowIdOrderByNodeKey(workflowId).stream()
                .collect(Collectors.toMap(WorkflowNodeExecutionEntity::getNodeKey, WorkflowNodeExecutionEntity::getStatus));
    }

    @Override
    public void close() { executor.close(); }
}
