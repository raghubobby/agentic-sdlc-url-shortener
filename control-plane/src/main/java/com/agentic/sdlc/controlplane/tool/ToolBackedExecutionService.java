package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.*;
import com.agentic.sdlc.controlplane.governance.NodeKind;
import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import com.agentic.sdlc.controlplane.workflow.WorkflowRunEntity;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Service
public class ToolBackedExecutionService {
    private final WorkspaceManager workspaces;
    private final WorkspaceAssemblyService assembler;
    private final WorkspaceAuditService workspaceAudit;
    private final ToolBroker broker;
    private final ArtifactExecutionRenderer renderer;

    public ToolBackedExecutionService(
            WorkspaceManager workspaces,
            WorkspaceAssemblyService assembler,
            WorkspaceAuditService workspaceAudit,
            ToolBroker broker,
            ArtifactExecutionRenderer renderer) {
        this.workspaces = workspaces;
        this.assembler = assembler;
        this.workspaceAudit = workspaceAudit;
        this.broker = broker;
        this.renderer = renderer;
    }

    public ToolExecutionSummary execute(
            WorkflowRunEntity run,
            AgentExecutionContext agentContext,
            EngineeringAgent agent,
            AgentResult agentResult) {
        List<ToolAction> actions = agentResult.output().toolActions();
        if (actions.isEmpty()) return ToolExecutionSummary.none();

        assertActionsDeclared(agentResult, actions);
        Path workspace;
        try {
            workspace = workspaces.freshWorkspace(
                    run.getId(), agentContext.node().key(), agentContext.attempt(), run.getBaseCommit());
            workspaceAudit.created(run.getId(), agentContext.node().key(), workspace, run.getBaseCommit());
        } catch (RuntimeException ex) {
            throw new ToolExecutionException(
                    FailureCategory.TRANSIENT_INFRASTRUCTURE,
                    "Could not prepare isolated engineering workspace: " + message(ex), false);
        }

        boolean changedWorkspace = false;
        List<ToolExecutionSummary.Step> steps = new ArrayList<>();
        String producedDiff = "";
        try {
            if (agentContext.node().kind() == NodeKind.VALIDATION
                    || agentContext.node().kind() == NodeKind.SECURITY_REVIEW) {
                try {
                    assembler.applyUpstreamChanges(run.getId(), agentContext.node().key(), workspace,
                            agentContext.upstreamArtifacts());
                } catch (RuntimeException ex) {
                    throw new ToolExecutionException(
                            FailureCategory.VALIDATION,
                            "Combined workspace could not be assembled from upstream evidence: " + message(ex), false);
                }
            }

            ToolExecutionContext toolContext = new ToolExecutionContext(
                    run.getId(), agentContext.node().key(), agentContext.attempt(),
                    agent.role(), agent.capabilities(), workspace);

            for (ToolAction action : actions) {
                ToolResult result;
                try {
                    result = broker.execute(toolContext, action);
                } catch (CapabilityViolationException ex) {
                    throw ex;
                } catch (RuntimeException ex) {
                    throw new ToolExecutionException(
                            operationalFailure(action.tool()),
                            action.tool() + " could not execute: " + message(ex), changedWorkspace);
                }
                if (action.tool() == ToolActionType.APPLY_PATCH && result.success()) changedWorkspace = true;
                steps.add(new ToolExecutionSummary.Step(
                        action.tool(), result.exitCode(), result.summary(), renderer.hash(result.output())));
                if (!result.success()) {
                    throw new ToolExecutionException(
                            resultFailure(action.tool()),
                            action.tool() + " failed: " + result.summary() + formatOutput(result.output()),
                            changedWorkspace);
                }
                if (action.tool() == ToolActionType.GENERATE_DIFF) producedDiff = result.output();
            }
            return new ToolExecutionSummary(steps, producedDiff);
        } finally {
            try {
                workspaces.discard(workspace);
                workspaceAudit.discarded(run.getId(), agentContext.node().key(), workspace);
            } catch (RuntimeException cleanupFailure) {
                // Workspace cleanup is itself a safety control. Surface it rather than silently leaking mutable state.
                throw new ToolExecutionException(
                        FailureCategory.TRANSIENT_INFRASTRUCTURE,
                        "Isolated workspace cleanup failed: " + message(cleanupFailure), true);
            }
        }
    }

    private void assertActionsDeclared(AgentResult result, List<ToolAction> actions) {
        for (ToolAction action : actions) {
            String required = broker.requiredCapability(action.tool()).name();
            if (!result.output().requestedCapabilities().contains(required)) {
                throw new CapabilityViolationException(java.util.Set.of(required));
            }
        }
    }

    private FailureCategory resultFailure(ToolActionType type) {
        return switch (type) {
            case RUN_BUILD, RUN_TESTS, RUN_SECURITY_SCAN, APPLY_PATCH -> FailureCategory.VALIDATION;
            case GENERATE_DIFF, READ_REPOSITORY_SNAPSHOT -> FailureCategory.UNKNOWN;
        };
    }

    private FailureCategory operationalFailure(ToolActionType type) {
        return switch (type) {
            case RUN_BUILD, RUN_TESTS, RUN_SECURITY_SCAN, GENERATE_DIFF, READ_REPOSITORY_SNAPSHOT ->
                    FailureCategory.TRANSIENT_INFRASTRUCTURE;
            case APPLY_PATCH -> FailureCategory.VALIDATION;
        };
    }

    private String formatOutput(String output) {
        if (output == null || output.isBlank()) {
            return "";
        }

        int max = 8000;

        if (output.length() <= max) {
            return "\n" + output;
        }

        return "\n...[earlier output truncated]...\n"
                + output.substring(output.length() - max);
    }

    private String message(Throwable throwable) {
        return throwable.getMessage() == null ? throwable.getClass().getSimpleName() : throwable.getMessage();
    }
}
