package com.agentic.sdlc.controlplane.artifact;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "artifact",
        uniqueConstraints = @UniqueConstraint(name = "uk_artifact_node_version",
                columnNames = {"workflow_id", "node_key", "version"}))
public class ArtifactEntity {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_key", nullable = false, length = 120)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ArtifactType type;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ArtifactStatus status;

    @Column(name = "content_hash", nullable = false, length = 64)
    private String contentHash;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ArtifactEntity() {}

    public ArtifactEntity(
            UUID id, UUID workflowId, String nodeKey, ArtifactType type, int version,
            String contentHash, String content, String createdBy) {
        this.id = id;
        this.workflowId = workflowId;
        this.nodeKey = nodeKey;
        this.type = type;
        this.version = version;
        this.status = ArtifactStatus.ACTIVE;
        this.contentHash = contentHash;
        this.content = content;
        this.createdBy = createdBy;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getNodeKey() { return nodeKey; }
    public ArtifactType getType() { return type; }
    public int getVersion() { return version; }
    public ArtifactStatus getStatus() { return status; }
    public String getContentHash() { return contentHash; }
    public String getContent() { return content; }
    public String getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }

    public void markStale() {
        this.status = ArtifactStatus.STALE;
    }
}
