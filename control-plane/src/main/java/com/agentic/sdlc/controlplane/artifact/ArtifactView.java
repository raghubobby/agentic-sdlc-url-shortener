package com.agentic.sdlc.controlplane.artifact;

import java.time.Instant;
import java.util.UUID;

public record ArtifactView(
        UUID id,
        UUID workflowId,
        String nodeKey,
        ArtifactType type,
        int version,
        ArtifactStatus status,
        String contentHash,
        String content,
        String createdBy,
        Instant createdAt
) {
    static ArtifactView from(ArtifactEntity entity) {
        return new ArtifactView(
                entity.getId(), entity.getWorkflowId(), entity.getNodeKey(), entity.getType(),
                entity.getVersion(), entity.getStatus(), entity.getContentHash(), entity.getContent(),
                entity.getCreatedBy(), entity.getCreatedAt());
    }
}
