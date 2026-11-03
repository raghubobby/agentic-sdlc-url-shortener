package com.agentic.sdlc.controlplane.execution;

import com.agentic.sdlc.controlplane.agent.*;
import com.agentic.sdlc.controlplane.artifact.ArtifactRegistryService;
import com.agentic.sdlc.controlplane.clarification.ClarificationService;
import com.agentic.sdlc.controlplane.artifact.ArtifactView;
import com.agentic.sdlc.controlplane.llm.ModelGatewayException;
import com.agentic.sdlc.controlplane.llm.ModelRefusalException;
import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import com.agentic.sdlc.controlplane.tool.*;
import com.agentic.sdlc.controlplane.workflow.WorkflowRunEntity;
import com.agentic.sdlc.controlplane.workflow.WorkflowRunRepository;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class AgentNodeWorkExecutor implements NodeWorkExecutor {
    private final WorkflowRunRepository workflowRunRepository;
    private final ArtifactRegistryService artifactRegistryService;
    private final AgentRegistry agentRegistry;
    private final AgentAuditService auditService;
    private final ToolBroker toolBroker;
    private final WorkspaceManager workspaceManager;
    private final ToolBackedExecutionService toolExecutionService;
    private final ArtifactExecutionRenderer artifactRenderer;
    private final ClarificationService clarificationService;

    public AgentNodeWorkExecutor(
            WorkflowRunRepository workflowRunRepository,
            ArtifactRegistryService artifactRegistryService,
            AgentRegistry agentRegistry,
            AgentAuditService auditService,
            ToolBroker toolBroker,
            WorkspaceManager workspaceManager,
            ToolBackedExecutionService toolExecutionService,
            ArtifactExecutionRenderer artifactRenderer,
            ClarificationService clarificationService) {
        this.workflowRunRepository = workflowRunRepository;
        this.artifactRegistryService = artifactRegistryService;
        this.agentRegistry = agentRegistry;
        this.auditService = auditService;
        this.toolBroker = toolBroker;
        this.workspaceManager = workspaceManager;
        this.toolExecutionService = toolExecutionService;
        this.artifactRenderer = artifactRenderer;
        this.clarificationService = clarificationService;
    }

    @Override
    public NodeWorkResult execute(NodeExecutionContext context) {
        WorkflowRunEntity run = workflowRunRepository.findById(context.workflowId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow: " + context.workflowId()));
        EngineeringAgent agent = agentRegistry.resolve(context.definition());
        List<ArtifactView> upstream = context.definition().dependencies().stream()
                .sorted()
                .map(key -> {
                    var artifact = artifactRegistryService.activeArtifact(context.workflowId(), key);
                    return new ArtifactView(
                            artifact.getId(), artifact.getWorkflowId(), artifact.getNodeKey(), artifact.getType(),
                            artifact.getVersion(), artifact.getStatus(), artifact.getContentHash(), artifact.getContent(),
                            artifact.getCreatedBy(), artifact.getCreatedAt());
                })
                .toList();

        try {
            String repositoryEvidence = repositoryEvidence(context, agent);
            AgentExecutionContext agentContext = new AgentExecutionContext(
                    context.workflowId(), context.definition(), context.attempt(),
                    run.getScenario(), run.getRequirement(), upstream, repositoryEvidence);
            auditService.started(context.workflowId(), context.definition().key(), agent.role(), agent.capabilities());

            AgentResult result = agent.execute(agentContext);
            auditService.completed(context.workflowId(), context.definition().key(), result);

            ToolExecutionSummary tools = toolExecutionService.execute(run, agentContext, agent, result);
            if (isMateriallyAmbiguous(run, context, result)) {
                clarificationService.request(context.workflowId(), context.definition().key(), result.output().ambiguities());
            }
            String artifactContent = artifactRenderer.render(result, tools);
            String createdBy = "agent:%s@%s/%s".formatted(
                    result.role().name().toLowerCase(), result.provider(), result.model());
            return NodeWorkResult.success(result.artifactType(), artifactContent, createdBy);
        } catch (CapabilityViolationException ex) {
            auditService.capabilityDenied(context.workflowId(), context.definition().key(), agent.role(), ex);
            return NodeWorkResult.failure(FailureCategory.POLICY, ex.getMessage(), false);
        } catch (ModelRefusalException ex) {
            auditService.refused(context.workflowId(), context.definition().key(), agent.role(), ex.getMessage());
            return NodeWorkResult.failure(FailureCategory.POLICY, ex.getMessage(), false);
        } catch (ModelGatewayException ex) {
            return NodeWorkResult.failure(ex.failureCategory(), ex.getMessage(), false);
        } catch (ToolExecutionException ex) {
            return NodeWorkResult.failure(ex.failureCategory(), ex.getMessage(), ex.sideEffectsPossible());
        } catch (IllegalArgumentException ex) {
            return NodeWorkResult.failure(FailureCategory.VALIDATION, ex.getMessage(), false);
        }
    }

    private boolean isMateriallyAmbiguous(WorkflowRunEntity run, NodeExecutionContext context, AgentResult result) {
        return "ambiguous".equalsIgnoreCase(run.getScenario())
                && "requirements".equals(context.definition().key())
                && !result.output().ambiguities().isEmpty();
    }

    private String repositoryEvidence(NodeExecutionContext context, EngineeringAgent agent) {
        if (!agent.capabilities().allowed().contains(AgentCapability.READ_REPOSITORY)) {
            return "(agent has no READ_REPOSITORY capability)";
        }
        ToolExecutionContext toolContext = new ToolExecutionContext(
                context.workflowId(), context.definition().key(), context.attempt(), agent.role(),
                agent.capabilities(), workspaceManager.repositoryRoot());
        try {
            ToolResult result = toolBroker.execute(toolContext,
                    new ToolAction(ToolActionType.READ_REPOSITORY_SNAPSHOT, null, null, "url-shortener"));
            if (!result.success()) {
                throw new ToolExecutionException(
                        FailureCategory.TRANSIENT_INFRASTRUCTURE,
                        "Could not capture repository evidence: " + result.summary(), false);
            }
            return result.output();
        } catch (CapabilityViolationException ex) {
            throw ex;
        } catch (ToolExecutionException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            throw new ToolExecutionException(
                    FailureCategory.TRANSIENT_INFRASTRUCTURE,
                    "Could not capture repository evidence: "
                            + (ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage()), false);
        }
    }
}
