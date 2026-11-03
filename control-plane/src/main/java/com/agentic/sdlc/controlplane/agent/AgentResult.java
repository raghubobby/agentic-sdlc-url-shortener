package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.artifact.ArtifactType;

public record AgentResult(
        AgentRole role,
        String provider,
        String model,
        AgentOutput output,
        ArtifactType artifactType,
        String artifactContent
) {}
