package com.agentic.sdlc.controlplane.observability;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ControlPlaneMetrics {
    private final MeterRegistry registry;
    private final Map<UUID, Long> workflowStarts = new ConcurrentHashMap<>();
    private final Map<String, Long> nodeStarts = new ConcurrentHashMap<>();

    public ControlPlaneMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordAfterCommit(UUID workflowId, String nodeKey, AuditEventType eventType) {
        increment(eventType);
        switch (eventType) {
            case WORKFLOW_STARTED -> workflowStarts.putIfAbsent(workflowId, System.nanoTime());
            case WORKFLOW_COMPLETED, WORKFLOW_FAILED -> finishWorkflow(workflowId, eventType);
            case NODE_STARTED -> nodeStarts.put(nodeKey(workflowId, nodeKey), System.nanoTime());
            case NODE_RETRY_SCHEDULED -> finishNode(workflowId, nodeKey, "retry");
            case NODE_SUCCEEDED -> finishNode(workflowId, nodeKey, "success");
            case NODE_FAILED -> finishNode(workflowId, nodeKey, "failure");
            default -> { }
        }
    }

    private void increment(AuditEventType eventType) {
        Counter.builder("agentic.audit.events")
                .description("Committed control-plane audit events")
                .tag("event", eventType.name().toLowerCase())
                .register(registry)
                .increment();
    }

    private void finishWorkflow(UUID workflowId, AuditEventType outcome) {
        Long started = workflowStarts.remove(workflowId);
        if (started == null) return;
        Timer.builder("agentic.workflow.duration")
                .description("End-to-end workflow duration")
                .tag("outcome", outcome == AuditEventType.WORKFLOW_COMPLETED ? "success" : "failure")
                .publishPercentileHistogram()
                .register(registry)
                .record(Duration.ofNanos(System.nanoTime() - started));
    }

    private void finishNode(UUID workflowId, String nodeKey, String outcome) {
        Long started = nodeStarts.remove(nodeKey(workflowId, nodeKey));
        if (started == null) return;
        Timer.builder("agentic.node.duration")
                .description("Node attempt duration")
                .tag("node", nodeKey == null ? "unknown" : nodeKey)
                .tag("outcome", outcome)
                .publishPercentileHistogram()
                .register(registry)
                .record(Duration.ofNanos(System.nanoTime() - started));
    }

    private String nodeKey(UUID workflowId, String nodeKey) {
        return workflowId + ":" + (nodeKey == null ? "unknown" : nodeKey);
    }
}
