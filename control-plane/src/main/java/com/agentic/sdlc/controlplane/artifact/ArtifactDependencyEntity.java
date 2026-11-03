package com.agentic.sdlc.controlplane.artifact;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "artifact_dependency",
        uniqueConstraints = @UniqueConstraint(name = "uk_artifact_dependency",
                columnNames = {"artifact_id", "depends_on_artifact_id"}))
public class ArtifactDependencyEntity {
    @Id
    private UUID id;

    @Column(name = "artifact_id", nullable = false)
    private UUID artifactId;

    @Column(name = "depends_on_artifact_id", nullable = false)
    private UUID dependsOnArtifactId;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    protected ArtifactDependencyEntity() {}

    public ArtifactDependencyEntity(UUID artifactId, UUID dependsOnArtifactId, String inputHash) {
        this.id = UUID.randomUUID();
        this.artifactId = artifactId;
        this.dependsOnArtifactId = dependsOnArtifactId;
        this.inputHash = inputHash;
    }

    public UUID getId() { return id; }
    public UUID getArtifactId() { return artifactId; }
    public UUID getDependsOnArtifactId() { return dependsOnArtifactId; }
    public String getInputHash() { return inputHash; }
}
