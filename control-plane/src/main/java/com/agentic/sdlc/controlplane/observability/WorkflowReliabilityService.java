package com.agentic.sdlc.controlplane.observability;

import com.agentic.sdlc.controlplane.approval.ApprovalRequestEntity;
import com.agentic.sdlc.controlplane.approval.ApprovalRequestRepository;
import com.agentic.sdlc.controlplane.approval.ApprovalStatus;
import com.agentic.sdlc.controlplane.artifact.ArtifactRepository;
import com.agentic.sdlc.controlplane.audit.AuditEventEntity;
import com.agentic.sdlc.controlplane.audit.AuditEventRepository;
import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.clarification.ClarificationRequestEntity;
import com.agentic.sdlc.controlplane.clarification.ClarificationRequestRepository;
import com.agentic.sdlc.controlplane.llm.ModelInvocationRepository;
import com.agentic.sdlc.controlplane.replan.ReplanRecordRepository;
import com.agentic.sdlc.controlplane.tool.ToolExecutionRepository;
import com.agentic.sdlc.controlplane.workflow.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WorkflowReliabilityService {
    private final WorkflowRunRepository runRepository;
    private final WorkflowNodeExecutionRepository nodeRepository;
    private final AuditEventRepository auditRepository;
    private final ApprovalRequestRepository approvalRepository;
    private final ClarificationRequestRepository clarificationRepository;
    private final ReplanRecordRepository replanRepository;
    private final ModelInvocationRepository modelRepository;
    private final ToolExecutionRepository toolRepository;
    private final ArtifactRepository artifactRepository;

    public WorkflowReliabilityService(
            WorkflowRunRepository runRepository,
            WorkflowNodeExecutionRepository nodeRepository,
            AuditEventRepository auditRepository,
            ApprovalRequestRepository approvalRepository,
            ClarificationRequestRepository clarificationRepository,
            ReplanRecordRepository replanRepository,
            ModelInvocationRepository modelRepository,
            ToolExecutionRepository toolRepository,
            ArtifactRepository artifactRepository) {
        this.runRepository = runRepository;
        this.nodeRepository = nodeRepository;
        this.auditRepository = auditRepository;
        this.approvalRepository = approvalRepository;
        this.clarificationRepository = clarificationRepository;
        this.replanRepository = replanRepository;
        this.modelRepository = modelRepository;
        this.toolRepository = toolRepository;
        this.artifactRepository = artifactRepository;
    }


    @Transactional(readOnly = true)
    public SystemReliabilitySnapshot systemSnapshot() {
        long completed = runRepository.countByStatus(WorkflowStatus.COMPLETED);
        long failed = runRepository.countByStatus(WorkflowStatus.FAILED);
        long total = runRepository.count();
        long terminal = completed + failed;
        long attempts = auditRepository.countByEventType(AuditEventType.NODE_STARTED);
        long retries = auditRepository.countByEventType(AuditEventType.NODE_RETRY_SCHEDULED);
        long compensations = auditRepository.countByEventType(AuditEventType.COMPENSATION_STARTED);
        return new SystemReliabilitySnapshot(
                total, completed, failed, Math.max(0, total - terminal),
                ratio(completed, terminal),
                attempts, retries, ratio(retries, attempts),
                compensations, ratio(compensations, attempts),
                replanRepository.count(),
                auditRepository.countByEventType(AuditEventType.POLICY_DENIED),
                modelRepository.count(), toolRepository.count(), artifactRepository.count());
    }

    @Transactional(readOnly = true)
    public WorkflowReliabilitySnapshot snapshot(UUID workflowId) {
        WorkflowRunEntity run = runRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow: " + workflowId));
        List<WorkflowNodeExecutionEntity> nodes = nodeRepository.findByWorkflowIdOrderByNodeKey(workflowId);
        List<AuditEventEntity> events = auditRepository.findByWorkflowIdOrderByEventSequence(workflowId);
        List<ApprovalRequestEntity> approvals = approvalRepository.findByWorkflowIdOrderByRequestedAtAsc(workflowId);
        List<ClarificationRequestEntity> clarifications = clarificationRepository.findByWorkflowIdOrderByRequestedAtAsc(workflowId);

        int succeeded = (int) nodes.stream().filter(n -> n.getStatus() == NodeStatus.SUCCEEDED).count();
        int failed = (int) nodes.stream().filter(n -> n.getStatus() == NodeStatus.FAILED).count();
        int terminal = succeeded + failed;
        double successRate = terminal == 0 ? 0.0 : (double) succeeded / terminal;

        int retries = count(events, AuditEventType.NODE_RETRY_SCHEDULED);
        List<Long> recoveryTimes = recoveryTimes(events);
        Long mttr = recoveryTimes.isEmpty() ? null : Math.round(recoveryTimes.stream().mapToLong(Long::longValue).average().orElse(0));

        long endToEnd = elapsed(run.getStartedAt(), run.getCompletedAt() == null ? Instant.now() : run.getCompletedAt());
        Long approvalWait = averageResolvedDuration(approvals.stream()
                .filter(a -> a.getResolvedAt() != null && a.getStatus() != ApprovalStatus.SUPERSEDED)
                .map(a -> elapsed(a.getRequestedAt(), a.getResolvedAt())).toList());
        Long clarificationWait = averageResolvedDuration(clarifications.stream()
                .filter(c -> c.getResolvedAt() != null)
                .map(c -> elapsed(c.getRequestedAt(), c.getResolvedAt())).toList());

        List<String> risks = nodes.stream()
                .filter(n -> n.getStatus() == NodeStatus.FAILED || n.getStatus() == NodeStatus.WAITING_FOR_APPROVAL)
                .map(n -> n.getNodeKey() + ":" + n.getStatus())
                .sorted()
                .toList();

        return new WorkflowReliabilitySnapshot(
                workflowId,
                run.getScenario(),
                run.getStatus(),
                run.getPlanRevision(),
                endToEnd,
                nodes.size(),
                succeeded,
                failed,
                successRate,
                retries,
                recoveryTimes.size(),
                mttr,
                count(events, AuditEventType.COMPENSATION_STARTED),
                replanRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId).size(),
                count(events, AuditEventType.POLICY_DENIED),
                approvals.size(),
                approvalWait,
                clarifications.size(),
                clarificationWait,
                modelRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId).size(),
                toolRepository.findByWorkflowIdOrderByStartedAtAsc(workflowId).size(),
                artifactRepository.findByWorkflowIdOrderByCreatedAtAsc(workflowId).size(),
                risks);
    }

    private double ratio(long numerator, long denominator) {
        return denominator == 0 ? 0.0 : (double) numerator / denominator;
    }

    private int count(List<AuditEventEntity> events, AuditEventType type) {
        return (int) events.stream().filter(e -> e.getEventType() == type).count();
    }

    private List<Long> recoveryTimes(List<AuditEventEntity> events) {
        Map<String, Instant> firstRetry = new HashMap<>();
        List<Long> recovered = new ArrayList<>();
        for (AuditEventEntity event : events) {
            String node = event.getNodeKey();
            if (node == null) continue;
            if (event.getEventType() == AuditEventType.NODE_RETRY_SCHEDULED) {
                firstRetry.putIfAbsent(node, event.getCreatedAt());
            } else if (event.getEventType() == AuditEventType.NODE_SUCCEEDED) {
                Instant started = firstRetry.remove(node);
                if (started != null) recovered.add(elapsed(started, event.getCreatedAt()));
            }
        }
        return recovered;
    }

    private Long averageResolvedDuration(List<Long> values) {
        if (values.isEmpty()) return null;
        return Math.round(values.stream().mapToLong(Long::longValue).average().orElse(0));
    }

    private long elapsed(Instant start, Instant end) {
        if (start == null || end == null) return 0;
        return Math.max(0, Duration.between(start, end).toMillis());
    }
}
