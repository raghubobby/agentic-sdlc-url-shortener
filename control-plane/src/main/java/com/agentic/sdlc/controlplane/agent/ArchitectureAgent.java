package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.artifact.ArtifactType;
import com.agentic.sdlc.controlplane.governance.NodeKind;
import com.agentic.sdlc.controlplane.llm.ModelGateway;
import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;
import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class ArchitectureAgent extends AbstractModelBackedAgent {
    public ArchitectureAgent(ModelGateway gateway, AgentPromptFactory promptFactory, ObjectMapper objectMapper) {
        super(gateway, promptFactory, objectMapper, AgentRole.ARCHITECT, CapabilityEnvelope.readOnly());
    }

    @Override public boolean supports(WorkflowNodeDefinition node) { return node.kind() == NodeKind.ARCHITECTURE; }
    @Override protected ArtifactType artifactType(AgentExecutionContext context) { return ArtifactType.ARCHITECTURE_DECISION; }
}
