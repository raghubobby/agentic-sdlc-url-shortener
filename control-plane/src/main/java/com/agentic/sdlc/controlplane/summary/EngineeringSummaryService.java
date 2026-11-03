package com.agentic.sdlc.controlplane.summary;

import com.agentic.sdlc.controlplane.artifact.ArtifactRegistryService;
import com.agentic.sdlc.controlplane.artifact.ArtifactView;
import com.agentic.sdlc.controlplane.audit.AuditEventEntity;
import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.clarification.ClarificationService;
import com.agentic.sdlc.controlplane.clarification.ClarificationView;
import com.agentic.sdlc.controlplane.governance.GovernanceService;
import com.agentic.sdlc.controlplane.governance.PolicyDecision;
import com.agentic.sdlc.controlplane.governance.PolicyEvaluationEntity;
import com.agentic.sdlc.controlplane.llm.ModelInvocationService;
import com.agentic.sdlc.controlplane.replan.ReplanService;
import com.agentic.sdlc.controlplane.replan.ReplanView;
import com.agentic.sdlc.controlplane.scenario.ScenarioBlueprint;
import com.agentic.sdlc.controlplane.scenario.ScenarioCatalog;
import com.agentic.sdlc.controlplane.tool.ToolExecutionAuditService;
import com.agentic.sdlc.controlplane.tool.ToolExecutionView;
import com.agentic.sdlc.controlplane.workflow.NodeStatus;
import com.agentic.sdlc.controlplane.workflow.WorkflowEngine;
import com.agentic.sdlc.controlplane.workflow.WorkflowSnapshot;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class EngineeringSummaryService {
    private final WorkflowEngine workflowEngine;
    private final ArtifactRegistryService artifacts;
    private final ModelInvocationService models;
    private final ToolExecutionAuditService tools;
    private final GovernanceService governance;
    private final ReplanService replans;
    private final ClarificationService clarifications;
    private final AuditService audit;
    private final ScenarioCatalog scenarios;

    public EngineeringSummaryService(
            WorkflowEngine workflowEngine,
            ArtifactRegistryService artifacts,
            ModelInvocationService models,
            ToolExecutionAuditService tools,
            GovernanceService governance,
            ReplanService replans,
            ClarificationService clarifications,
            AuditService audit,
            ScenarioCatalog scenarios) {
        this.workflowEngine = workflowEngine;
        this.artifacts = artifacts;
        this.models = models;
        this.tools = tools;
        this.governance = governance;
        this.replans = replans;
        this.clarifications = clarifications;
        this.audit = audit;
        this.scenarios = scenarios;
    }

    public EngineeringSummary build(UUID workflowId) {
        WorkflowSnapshot workflow = workflowEngine.snapshot(workflowId);
        ScenarioBlueprint blueprint = scenarios.get(workflow.scenario());
        List<ArtifactView> artifactViews = artifacts.list(workflowId);
        var modelHistory = models.history(workflowId);
        List<ToolExecutionView> toolHistory = tools.workflow(workflowId);
        List<PolicyEvaluationEntity> policyHistory = governance.history(workflowId);
        List<ReplanView> replanHistory = replans.history(workflowId);
        List<ClarificationView> clarificationHistory = clarifications.history(workflowId);
        List<AuditEventEntity> timeline = audit.timeline(workflowId);

        long retries = count(timeline, AuditEventType.NODE_RETRY_SCHEDULED);
        long compensations = count(timeline, AuditEventType.COMPENSATION_STARTED);
        long approvalRequests = count(timeline, AuditEventType.APPROVAL_REQUESTED);
        long policyDenials = policyHistory.stream().filter(p -> p.getDecision() == PolicyDecision.DENY).count();
        long failureInjections = count(timeline, AuditEventType.FAILURE_INJECTED);
        long succeeded = workflow.nodes().stream().filter(n -> n.status() == NodeStatus.SUCCEEDED).count();
        long failed = workflow.nodes().stream().filter(n -> n.status() == NodeStatus.FAILED).count();

        EngineeringSummary.ExecutionMetrics metrics = new EngineeringSummary.ExecutionMetrics(
                workflow.nodes().size(), succeeded, failed, artifactViews.size(), modelHistory.size(),
                toolHistory.size(), retries, compensations, replanHistory.size(), clarificationHistory.size(),
                approvalRequests, policyDenials, failureInjections);

        List<String> decisions = new ArrayList<>();
        decisions.add("Workflow executed against pinned Git base commit " + workflow.baseCommit());
        decisions.add("AI reasoning remained subordinate to deterministic policy, capability and tool authorization.");
        decisions.add("Risk-based policy evaluations recorded: " + policyHistory.size());
        if (!clarificationHistory.isEmpty()) {
            decisions.add("Material ambiguity required human clarification before downstream execution.");
        }
        if (!replanHistory.isEmpty()) {
            decisions.add("Upstream changes produced selective plan revisions rather than whole-workflow restart.");
        }

        List<String> validation = new ArrayList<>(blueprint.validationCriteria());
        validation.add("Engineering artifacts produced: " + artifactViews.size());
        validation.add("Governed tool executions recorded: " + toolHistory.size());
        validation.add("Model invocations traceable by prompt/response hash: " + modelHistory.size());

        List<String> risks = List.of(
                "Model-generated content may be incomplete or incorrect; deterministic tests, policy and human gates remain authoritative.",
                "The prototype uses a single control-plane service and PostgreSQL transaction boundary; horizontal scheduler coordination is a future concern.",
                "Tool execution is intentionally restricted to isolated worktrees and the url-shortener module, limiting blast radius.");

        List<String> limitations = List.of(
                "Deterministic model mode proves orchestration reproducibly but does not represent real-model reasoning quality.",
                "No autonomous merge, Git push, or production deployment capability is granted.",
                "Performance/load validation is scenario evidence, not a claim of internet-scale benchmarking.");

        String markdown = render(workflow, blueprint, metrics, decisions, validation, risks, limitations,
                artifactViews, clarificationHistory, replanHistory);
        return new EngineeringSummary(
                workflow.id(), workflow.scenario(), workflow.requirement(), workflow.status(), workflow.planRevision(),
                workflow.baseCommit(), Instant.now(), metrics, decisions, validation, risks, limitations, markdown);
    }

    private long count(List<AuditEventEntity> timeline, AuditEventType type) {
        return timeline.stream().filter(event -> event.getEventType() == type).count();
    }

    private String render(
            WorkflowSnapshot workflow,
            ScenarioBlueprint blueprint,
            EngineeringSummary.ExecutionMetrics metrics,
            List<String> decisions,
            List<String> validation,
            List<String> risks,
            List<String> limitations,
            List<ArtifactView> artifacts,
            List<ClarificationView> clarifications,
            List<ReplanView> replans) {
        StringBuilder out = new StringBuilder();
        out.append("# Final Engineering Summary\n\n")
                .append("## Scenario\n")
                .append("**").append(blueprint.title()).append("**\n\n")
                .append("Requirement: ").append(workflow.requirement()).append("\n\n")
                .append("Status: ").append(workflow.status()).append(" | Plan revision: ")
                .append(workflow.planRevision()).append(" | Base commit: `").append(workflow.baseCommit()).append("`\n\n")
                .append("## Plan and rationale\n")
                .append(blueprint.engineeringIntent()).append("\n\n");
        bullets(out, decisions);
        out.append("\n## Evidence\n")
                .append("Artifacts: ").append(metrics.artifacts())
                .append(" | Model invocations: ").append(metrics.modelInvocations())
                .append(" | Tool executions: ").append(metrics.toolExecutions())
                .append(" | Replans: ").append(metrics.replans())
                .append(" | Clarifications: ").append(metrics.clarifications())
                .append(" | Injected demo failures: ").append(metrics.failureInjections()).append("\n\n");
        artifacts.stream().filter(a -> a.status().name().equals("ACTIVE")).forEach(a -> out.append("- ")
                .append(a.nodeKey()).append(" -> ").append(a.type()).append(" v").append(a.version())
                .append(" hash=`").append(a.contentHash()).append("`\n"));
        if (!clarifications.isEmpty()) {
            out.append("\n## Human clarification lineage\n");
            clarifications.forEach(c -> out.append("- revision ").append(c.planRevision()).append(" ")
                    .append(c.status()).append(" by ").append(c.resolvedBy() == null ? "pending" : c.resolvedBy())
                    .append("\n"));
        }
        if (!replans.isEmpty()) {
            out.append("\n## Replanning\n");
            replans.forEach(r -> out.append("- revision ").append(r.fromRevision()).append(" -> ")
                    .append(r.toRevision()).append("; invalidated=").append(r.invalidatedNodes())
                    .append("; preserved=").append(r.preservedNodes()).append("\n"));
        }
        out.append("\n## Validation\n"); bullets(out, validation);
        out.append("\n## Risks and trade-offs\n"); bullets(out, risks);
        out.append("\n## Assumptions and limitations\n"); bullets(out, limitations);
        return out.toString();
    }

    private void bullets(StringBuilder out, List<String> values) {
        values.forEach(value -> out.append("- ").append(value).append("\n"));
    }
}
