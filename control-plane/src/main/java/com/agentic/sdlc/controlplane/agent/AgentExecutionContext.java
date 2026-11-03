package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.artifact.ArtifactView;
import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;

import java.util.List;
import java.util.UUID;

public record AgentExecutionContext(
        UUID workflowId,
        WorkflowNodeDefinition node,
        int attempt,
        String scenario,
        String requirement,
        List<ArtifactView> upstreamArtifacts,
        String repositoryEvidence
) {
    public AgentExecutionContext {
        upstreamArtifacts = List.copyOf(upstreamArtifacts);
        repositoryEvidence = repositoryEvidence == null ? "(repository evidence not requested)" : repositoryEvidence;
    }
}
