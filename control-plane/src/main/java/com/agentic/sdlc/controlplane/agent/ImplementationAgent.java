package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.artifact.ArtifactType;
import com.agentic.sdlc.controlplane.governance.NodeKind;
import com.agentic.sdlc.controlplane.llm.ModelGateway;
import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.EnumSet;

@Component
public class ImplementationAgent extends AbstractModelBackedAgent {
    public ImplementationAgent(ModelGateway gateway, AgentPromptFactory promptFactory, ObjectMapper objectMapper) {
        super(gateway, promptFactory, objectMapper, AgentRole.IMPLEMENTER,
                new CapabilityEnvelope(
                        EnumSet.of(
                                AgentCapability.READ_REQUIREMENT,
                                AgentCapability.READ_ARTIFACTS,
                                AgentCapability.READ_REPOSITORY,
                                AgentCapability.WRITE_ENGINEERING_ARTIFACT,
                                AgentCapability.WRITE_WORKTREE,
                                AgentCapability.RUN_BUILD,
                                AgentCapability.RUN_TESTS,
                                AgentCapability.GENERATE_DIFF),
                        EnumSet.of(
                                AgentCapability.RUN_SHELL,
                                AgentCapability.WRITE_MAIN,
                                AgentCapability.GIT_PUSH,
                                AgentCapability.DEPLOY_PRODUCTION)));
    }

    @Override public boolean supports(WorkflowNodeDefinition node) { return node.kind() == NodeKind.IMPLEMENTATION; }
    @Override protected ArtifactType artifactType(AgentExecutionContext context) { return ArtifactType.CODE_CHANGE; }
}
