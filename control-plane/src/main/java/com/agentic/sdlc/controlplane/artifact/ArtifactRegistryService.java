package com.agentic.sdlc.controlplane.artifact;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class ArtifactRegistryService {
    private final ArtifactRepository artifactRepository;
    private final ArtifactDependencyRepository dependencyRepository;
    private final ArtifactHasher hasher;
    private final AuditService auditService;

    public ArtifactRegistryService(
            ArtifactRepository artifactRepository,
            ArtifactDependencyRepository dependencyRepository,
            ArtifactHasher hasher,
            AuditService auditService) {
        this.artifactRepository = artifactRepository;
        this.dependencyRepository = dependencyRepository;
        this.hasher = hasher;
        this.auditService = auditService;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ArtifactEntity publish(
            UUID workflowId, String nodeKey, Set<String> dependencyNodeKeys,
            ArtifactType type, String content, String createdBy) {

        int version = artifactRepository.findTopByWorkflowIdAndNodeKeyOrderByVersionDesc(workflowId, nodeKey)
                .map(existing -> existing.getVersion() + 1)
                .orElse(1);

        ArtifactEntity artifact = new ArtifactEntity(
                UUID.randomUUID(), workflowId, nodeKey, type, version,
                hasher.sha256(content), content, createdBy);
        artifactRepository.save(artifact);

        dependencyNodeKeys.stream().sorted().forEach(dependencyNodeKey -> {
            ArtifactEntity input = artifactRepository
                    .findTopByWorkflowIdAndNodeKeyAndStatusOrderByVersionDesc(
                            workflowId, dependencyNodeKey, ArtifactStatus.ACTIVE)
                    .orElseThrow(() -> new IllegalStateException(
                            "Missing active artifact for dependency node: " + dependencyNodeKey));
            dependencyRepository.save(new ArtifactDependencyEntity(
                    artifact.getId(), input.getId(), input.getContentHash()));
        });

        auditService.append(workflowId, nodeKey, AuditEventType.ARTIFACT_CREATED,
                "artifactId=%s;type=%s;version=%d;hash=%s".formatted(
                        artifact.getId(), type, version, artifact.getContentHash()));
        return artifact;
    }

    @Transactional(readOnly = true)
    public ArtifactEntity activeArtifact(UUID workflowId, String nodeKey) {
        return artifactRepository.findTopByWorkflowIdAndNodeKeyAndStatusOrderByVersionDesc(
                        workflowId, nodeKey, ArtifactStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing active artifact for node: " + nodeKey));
    }

    @Transactional(readOnly = true)
    public Set<String> downstreamActiveArtifactNodes(UUID sourceArtifactId) {
        Set<String> nodeKeys = new TreeSet<>();
        Set<UUID> visited = new HashSet<>();
        Deque<UUID> queue = new ArrayDeque<>();
        queue.add(sourceArtifactId);
        visited.add(sourceArtifactId);

        while (!queue.isEmpty()) {
            UUID current = queue.removeFirst();
            for (ArtifactDependencyEntity edge : dependencyRepository.findByDependsOnArtifactId(current)) {
                UUID dependentId = edge.getArtifactId();
                if (!visited.add(dependentId)) continue;
                ArtifactEntity dependent = artifactRepository.findById(dependentId)
                        .orElseThrow(() -> new IllegalStateException(
                                "Artifact dependency references missing artifact: " + dependentId));
                if (dependent.getStatus() == ArtifactStatus.ACTIVE) {
                    nodeKeys.add(dependent.getNodeKey());
                    queue.addLast(dependentId);
                }
            }
        }
        return Collections.unmodifiableSet(nodeKeys);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ArtifactEntity reviseSourceArtifact(
            UUID workflowId, String nodeKey, Set<String> dependencyNodeKeys,
            String revisedContent, String changedBy, int planRevision) {
        ArtifactEntity current = activeArtifact(workflowId, nodeKey);
        current.markStale();
        auditService.append(workflowId, nodeKey, AuditEventType.ARTIFACT_STALE,
                "artifactId=%s;version=%d;planRevision=%d;reason=source revised".formatted(
                        current.getId(), current.getVersion(), planRevision));

        ArtifactEntity revised = publish(
                workflowId, nodeKey, dependencyNodeKeys, current.getType(), revisedContent, changedBy);
        auditService.append(workflowId, nodeKey, AuditEventType.ARTIFACT_REVISED,
                "oldArtifactId=%s;newArtifactId=%s;version=%d;planRevision=%d".formatted(
                        current.getId(), revised.getId(), revised.getVersion(), planRevision));
        return revised;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markActiveArtifactsStale(UUID workflowId, Set<String> nodeKeys, int planRevision) {
        if (nodeKeys.isEmpty()) return;
        artifactRepository.findByWorkflowIdAndNodeKeyInAndStatus(workflowId, nodeKeys, ArtifactStatus.ACTIVE)
                .forEach(artifact -> {
                    artifact.markStale();
                    auditService.append(workflowId, artifact.getNodeKey(), AuditEventType.ARTIFACT_STALE,
                            "artifactId=%s;version=%d;planRevision=%d;reason=upstream invalidated".formatted(
                                    artifact.getId(), artifact.getVersion(), planRevision));
                });
    }

    @Transactional(readOnly = true)
    public List<ArtifactView> list(UUID workflowId) {
        return artifactRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId).stream()
                .map(ArtifactView::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArtifactLineage lineage(UUID artifactId) {
        return lineage(artifactId, new HashSet<>());
    }

    private ArtifactLineage lineage(UUID artifactId, Set<UUID> visited) {
        if (!visited.add(artifactId)) {
            throw new IllegalStateException("Artifact lineage contains a cycle at " + artifactId);
        }
        ArtifactEntity artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown artifact: " + artifactId));
        List<ArtifactLineage.ArtifactInput> inputs = dependencyRepository.findByArtifactId(artifactId).stream()
                .map(dep -> {
                    ArtifactLineage source = lineage(dep.getDependsOnArtifactId(), new HashSet<>(visited));
                    return new ArtifactLineage.ArtifactInput(
                            dep.getInputHash(),
                            dep.getInputHash().equals(source.artifact().contentHash()),
                            source);
                })
                .toList();
        return new ArtifactLineage(ArtifactView.from(artifact), inputs);
    }
}
