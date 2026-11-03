package com.agentic.sdlc.controlplane.execution;

import com.agentic.sdlc.controlplane.artifact.ArtifactType;
import com.agentic.sdlc.controlplane.recovery.FailureCategory;

public record NodeWorkResult(
        boolean success,
        FailureCategory failureCategory,
        String message,
        ArtifactType artifactType,
        String artifactContent,
        boolean sideEffectsPossible,
        String createdBy
) {
    public static NodeWorkResult success(ArtifactType type, String content) {
        return success(type, content, "node-work-executor");
    }

    public static NodeWorkResult success(ArtifactType type, String content, String createdBy) {
        return new NodeWorkResult(true, null, "success", type, content, false, createdBy);
    }

    public static NodeWorkResult failure(FailureCategory category, String message, boolean sideEffectsPossible) {
        return new NodeWorkResult(false, category, message, null, null, sideEffectsPossible, null);
    }
}
