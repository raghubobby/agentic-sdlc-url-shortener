package com.agentic.sdlc.controlplane.artifact;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ArtifactDependencyRepository extends JpaRepository<ArtifactDependencyEntity, UUID> {
    List<ArtifactDependencyEntity> findByArtifactId(UUID artifactId);
    List<ArtifactDependencyEntity> findByDependsOnArtifactId(UUID dependsOnArtifactId);
}
