package com.agentic.sdlc.controlplane.replan;

import com.agentic.sdlc.controlplane.approval.ApprovalService;
import com.agentic.sdlc.controlplane.artifact.ArtifactEntity;
import com.agentic.sdlc.controlplane.artifact.ArtifactHasher;
import com.agentic.sdlc.controlplane.artifact.ArtifactRegistryService;
import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.workflow.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReplanService {
    private final WorkflowRunRepository runRepository;
    private final WorkflowNodeExecutionRepository nodeRepository;
    private final ArtifactRegistryService artifactRegistryService;
    private final ArtifactHasher artifactHasher;
    private final ApprovalService approvalService;
    private final ReplanRecordRepository replanRepository;
    private final AuditService auditService;
    private final WorkflowDefinition definition = WorkflowDefinition.defaultEngineeringWorkflow();

    public ReplanService(
            WorkflowRunRepository runRepository,
            WorkflowNodeExecutionRepository nodeRepository,
            ArtifactRegistryService artifactRegistryService,
            ArtifactHasher artifactHasher,
            ApprovalService approvalService,
            ReplanRecordRepository replanRepository,
            AuditService auditService) {
        this.runRepository = runRepository;
        this.nodeRepository = nodeRepository;
        this.artifactRegistryService = artifactRegistryService;
        this.artifactHasher = artifactHasher;
        this.approvalService = approvalService;
        this.replanRepository = replanRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ReplanView revise(
            UUID workflowId,
            String changeId,
            String sourceNodeKey,
            String revisedArtifactContent,
            String reason,
            String changedBy) {
        return revise(workflowId, changeId, sourceNodeKey, revisedArtifactContent,
                sourceNodeKey.equals("requirements") ? revisedArtifactContent : null, reason, changedBy);
    }

    @Transactional
    public ReplanView revise(
            UUID workflowId,
            String changeId,
            String sourceNodeKey,
            String revisedArtifactContent,
            String revisedRequirement,
            String reason,
            String changedBy) {

        ReplanRecordEntity prior = replanRepository.findByWorkflowIdAndChangeId(workflowId, changeId).orElse(null);
        if (prior != null) return ReplanView.from(prior);

        WorkflowRunEntity run = runRepository.findForUpdateById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow: " + workflowId));

        // Re-check after acquiring the per-workflow lock. A concurrent request with the same
        // changeId may have committed while this request was waiting for the lock.
        prior = replanRepository.findByWorkflowIdAndChangeId(workflowId, changeId).orElse(null);
        if (prior != null) return ReplanView.from(prior);

        if (run.getStatus() == WorkflowStatus.RUNNING) {
            throw new IllegalStateException("Cannot replan while workflow nodes are actively running");
        }

        definition.node(sourceNodeKey);
        WorkflowNodeExecutionEntity sourceNode = nodeRepository.findByWorkflowIdAndNodeKey(workflowId, sourceNodeKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown source node: " + sourceNodeKey));
        if (sourceNode.getStatus() != NodeStatus.SUCCEEDED) {
            throw new IllegalStateException("Source node must have a successful artifact before revision: " + sourceNodeKey);
        }

        ArtifactEntity current = artifactRegistryService.activeArtifact(workflowId, sourceNodeKey);
        String newHash = artifactHasher.sha256(revisedArtifactContent);
        int fromRevision = run.getPlanRevision();

        if (current.getContentHash().equals(newHash)) {
            ReplanRecordEntity noOp = new ReplanRecordEntity(
                    workflowId, changeId, fromRevision, fromRevision, sourceNodeKey,
                    current.getContentHash(), newHash, false, "", "", "", "", reason, changedBy);
            replanRepository.save(noOp);
            auditService.append(workflowId, sourceNodeKey, AuditEventType.REPLAN_SKIPPED,
                    "changeId=%s;reason=content hash unchanged;hash=%s".formatted(changeId, newHash));
            return ReplanView.from(noOp);
        }

        auditService.append(workflowId, sourceNodeKey, AuditEventType.REPLAN_STARTED,
                "changeId=%s;fromRevision=%d;reason=%s".formatted(changeId, fromRevision, reason));

        int toRevision = run.beginReplan(sourceNodeKey.equals("requirements") ? revisedRequirement : null);

        List<WorkflowNodeExecutionEntity> nodes = nodeRepository.findByWorkflowIdOrderByNodeKey(workflowId);
        Map<String, NodeStatus> beforeStatuses = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeExecutionEntity::getNodeKey, WorkflowNodeExecutionEntity::getStatus));

        Set<String> graphDescendants = new TreeSet<>(definition.descendantsOf(sourceNodeKey));
        Set<String> lineageImpacted = new TreeSet<>(artifactRegistryService.downstreamActiveArtifactNodes(current.getId()));

        // The graph is the conservative safety boundary. Provenance tells us which completed artifacts
        // actually consumed the old source; pending/failed descendants have no artifact edge yet but
        // still belong to the revised execution plan.
        Set<String> invalidated = new TreeSet<>(graphDescendants);
        Set<String> preserved = nodes.stream()
                .filter(node -> node.getStatus() == NodeStatus.SUCCEEDED)
                .map(WorkflowNodeExecutionEntity::getNodeKey)
                .filter(key -> !key.equals(sourceNodeKey))
                .filter(key -> !invalidated.contains(key))
                .collect(Collectors.toCollection(TreeSet::new));

        artifactRegistryService.reviseSourceArtifact(
                workflowId, sourceNodeKey, definition.node(sourceNodeKey).dependencies(),
                revisedArtifactContent, changedBy, toRevision);
        artifactRegistryService.markActiveArtifactsStale(workflowId, invalidated, toRevision);

        sourceNode.markExternallyRevised(toRevision);
        for (WorkflowNodeExecutionEntity node : nodes) {
            if (invalidated.contains(node.getNodeKey())) {
                NodeStatus priorStatus = beforeStatuses.get(node.getNodeKey());
                node.resetForReplan();
                auditService.append(workflowId, node.getNodeKey(), AuditEventType.NODE_INVALIDATED,
                        "planRevision=%d;previousStatus=%s;generation=%d".formatted(
                                toRevision, priorStatus, node.getGeneration()));
            }
        }

        approvalService.supersedePendingForReplan(workflowId, invalidated, toRevision);

        Map<String, NodeStatus> afterStatuses = nodes.stream()
                .collect(Collectors.toMap(WorkflowNodeExecutionEntity::getNodeKey, WorkflowNodeExecutionEntity::getStatus));
        List<String> nextReady = definition.readyNodes(afterStatuses).stream()
                .map(WorkflowNodeDefinition::key)
                .sorted()
                .toList();

        ReplanRecordEntity record = new ReplanRecordEntity(
                workflowId, changeId, fromRevision, toRevision, sourceNodeKey,
                current.getContentHash(), newHash, true,
                ReplanView.encode(lineageImpacted), ReplanView.encode(invalidated),
                ReplanView.encode(preserved), ReplanView.encode(nextReady),
                reason, changedBy);
        replanRepository.save(record);

        auditService.append(workflowId, sourceNodeKey, AuditEventType.REPLAN_COMPLETED,
                "changeId=%s;toRevision=%d;lineageImpacted=%s;invalidated=%s;preserved=%s;nextReady=%s".formatted(
                        changeId, toRevision, lineageImpacted, invalidated, preserved, nextReady));
        return ReplanView.from(record);
    }

    @Transactional(readOnly = true)
    public List<ReplanView> history(UUID workflowId) {
        return replanRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId).stream()
                .map(ReplanView::from)
                .toList();
    }
}
