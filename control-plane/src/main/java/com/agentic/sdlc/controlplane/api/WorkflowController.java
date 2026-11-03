package com.agentic.sdlc.controlplane.api;

import com.agentic.sdlc.controlplane.approval.ApprovalService;
import com.agentic.sdlc.controlplane.approval.ApprovalStatus;
import com.agentic.sdlc.controlplane.approval.ApprovalRequestEntity;
import com.agentic.sdlc.controlplane.approval.ApprovalVote;
import com.agentic.sdlc.controlplane.artifact.ArtifactLineage;
import com.agentic.sdlc.controlplane.artifact.ArtifactRegistryService;
import com.agentic.sdlc.controlplane.artifact.ArtifactView;
import com.agentic.sdlc.controlplane.audit.AuditEventEntity;
import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.governance.GovernanceService;
import com.agentic.sdlc.controlplane.governance.PolicyEvaluationEntity;
import com.agentic.sdlc.controlplane.replan.ReplanService;
import com.agentic.sdlc.controlplane.replan.ReplanView;
import com.agentic.sdlc.controlplane.llm.ModelInvocationEntity;
import com.agentic.sdlc.controlplane.llm.ModelInvocationService;
import com.agentic.sdlc.controlplane.workflow.WorkflowEngine;
import com.agentic.sdlc.controlplane.workflow.WorkflowSnapshot;
import com.agentic.sdlc.controlplane.tool.ToolExecutionAuditService;
import com.agentic.sdlc.controlplane.tool.ToolExecutionView;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowController {
    private final WorkflowEngine workflowEngine;
    private final AuditService auditService;
    private final ApprovalService approvalService;
    private final GovernanceService governanceService;
    private final ArtifactRegistryService artifactRegistryService;
    private final ReplanService replanService;
    private final ModelInvocationService modelInvocationService;
    private final ToolExecutionAuditService toolExecutionAuditService;

    public WorkflowController(
            WorkflowEngine workflowEngine,
            AuditService auditService,
            ApprovalService approvalService,
            GovernanceService governanceService,
            ArtifactRegistryService artifactRegistryService,
            ReplanService replanService,
            ModelInvocationService modelInvocationService,
            ToolExecutionAuditService toolExecutionAuditService) {
        this.workflowEngine = workflowEngine;
        this.auditService = auditService;
        this.approvalService = approvalService;
        this.governanceService = governanceService;
        this.artifactRegistryService = artifactRegistryService;
        this.replanService = replanService;
        this.modelInvocationService = modelInvocationService;
        this.toolExecutionAuditService = toolExecutionAuditService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowSnapshot create(@Valid @RequestBody CreateWorkflowRequest request) {
        UUID id = workflowEngine.create(request.scenario(), request.requirement());
        workflowEngine.runUntilBlocked(id);
        return workflowEngine.snapshot(id);
    }

    @GetMapping("/{workflowId}")
    public WorkflowSnapshot get(@PathVariable UUID workflowId) {
        return workflowEngine.snapshot(workflowId);
    }

    @PostMapping("/{workflowId}/approvals/{nodeKey}")
    public WorkflowSnapshot decide(
            @PathVariable UUID workflowId,
            @PathVariable String nodeKey,
            @Valid @RequestBody ApprovalDecisionRequest request) {
        ApprovalRequestEntity approval = approvalService.recordDecision(
                workflowId, nodeKey, request.decision(), request.decidedBy(), request.comment());
        if (approval.getStatus() != ApprovalStatus.PENDING) {
            workflowEngine.runUntilBlocked(workflowId);
        }
        return workflowEngine.snapshot(workflowId);
    }

    @GetMapping("/{workflowId}/approvals/{nodeKey}")
    public ApprovalService.ApprovalView approval(@PathVariable UUID workflowId, @PathVariable String nodeKey) {
        return approvalService.view(workflowId, nodeKey);
    }

    @GetMapping("/{workflowId}/audit")
    public List<AuditEventEntity> audit(@PathVariable UUID workflowId) {
        return auditService.timeline(workflowId);
    }

    @GetMapping("/{workflowId}/policy-evaluations")
    public List<PolicyEvaluationEntity> policyEvaluations(@PathVariable UUID workflowId) {
        return governanceService.history(workflowId);
    }


    @PostMapping("/{workflowId}/changes")
    public ChangeResponse revise(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ChangeRequest request) {
        ReplanView plan = replanService.revise(
                workflowId, request.changeId(), request.sourceNodeKey(),
                request.revisedArtifactContent(), request.reason(), request.changedBy());
        if (plan.applied() && Boolean.TRUE.equals(request.autoResume())) {
            workflowEngine.runUntilBlocked(workflowId);
        }
        return new ChangeResponse(plan, workflowEngine.snapshot(workflowId));
    }

    @PostMapping("/{workflowId}/resume")
    public WorkflowSnapshot resume(@PathVariable UUID workflowId) {
        workflowEngine.runUntilBlocked(workflowId);
        return workflowEngine.snapshot(workflowId);
    }

    @GetMapping("/{workflowId}/replans")
    public List<ReplanView> replans(@PathVariable UUID workflowId) {
        return replanService.history(workflowId);
    }


    @GetMapping("/{workflowId}/model-invocations")
    public List<ModelInvocationEntity> modelInvocations(@PathVariable UUID workflowId) {
        workflowEngine.snapshot(workflowId); // ownership/existence check
        return modelInvocationService.history(workflowId);
    }

    @GetMapping("/{workflowId}/tool-executions")
    public List<ToolExecutionView> toolExecutions(@PathVariable UUID workflowId) {
        workflowEngine.snapshot(workflowId); // ownership/existence check
        return toolExecutionAuditService.workflow(workflowId);
    }

    @GetMapping("/{workflowId}/artifacts")
    public List<ArtifactView> artifacts(@PathVariable UUID workflowId) {
        return artifactRegistryService.list(workflowId);
    }

    @GetMapping("/{workflowId}/artifacts/{artifactId}/lineage")
    public ArtifactLineage lineage(@PathVariable UUID workflowId, @PathVariable UUID artifactId) {
        ArtifactLineage lineage = artifactRegistryService.lineage(artifactId);
        if (!lineage.artifact().workflowId().equals(workflowId)) {
            throw new IllegalArgumentException("Artifact does not belong to workflow: " + workflowId);
        }
        return lineage;
    }

    public record CreateWorkflowRequest(@NotBlank String scenario, @NotBlank String requirement) {}

    public record ChangeRequest(
            @NotBlank String changeId,
            @NotBlank String sourceNodeKey,
            @NotBlank String revisedArtifactContent,
            @NotBlank String reason,
            @NotBlank String changedBy,
            Boolean autoResume
    ) {}

    public record ChangeResponse(ReplanView replan, WorkflowSnapshot workflow) {}

    public record ApprovalDecisionRequest(
            @NotNull ApprovalVote decision,
            @NotBlank String decidedBy,
            String comment
    ) {}
}
