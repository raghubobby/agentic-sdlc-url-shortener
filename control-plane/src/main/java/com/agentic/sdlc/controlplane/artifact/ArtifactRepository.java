package com.agentic.sdlc.controlplane.artifact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArtifactRepository extends JpaRepository<ArtifactEntity, UUID> {
    List<ArtifactEntity> findByWorkflowIdOrderByCreatedAtAsc(UUID workflowId);
    Optional<ArtifactEntity> findTopByWorkflowIdAndNodeKeyAndStatusOrderByVersionDesc(
            UUID workflowId, String nodeKey, ArtifactStatus status);
    Optional<ArtifactEntity> findTopByWorkflowIdAndNodeKeyOrderByVersionDesc(UUID workflowId, String nodeKey);
    List<ArtifactEntity> findByWorkflowIdAndNodeKeyInAndStatus(
            UUID workflowId, Collection<String> nodeKeys, ArtifactStatus status);
}
