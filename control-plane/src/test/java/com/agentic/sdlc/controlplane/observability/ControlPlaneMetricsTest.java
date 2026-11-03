package com.agentic.sdlc.controlplane.observability;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneMetricsTest {
    @Test
    void recordsCommittedEventsAndDurations() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        ControlPlaneMetrics metrics = new ControlPlaneMetrics(registry);
        UUID workflowId = UUID.randomUUID();

        metrics.recordAfterCommit(workflowId, null, AuditEventType.WORKFLOW_STARTED);
        metrics.recordAfterCommit(workflowId, "requirements", AuditEventType.NODE_STARTED);
        metrics.recordAfterCommit(workflowId, "requirements", AuditEventType.NODE_SUCCEEDED);
        metrics.recordAfterCommit(workflowId, null, AuditEventType.WORKFLOW_COMPLETED);

        assertThat(registry.get("agentic.audit.events").tag("event", "workflow_completed").counter().count())
                .isEqualTo(1.0);
        assertThat(registry.get("agentic.workflow.duration").tag("outcome", "success").timer().count())
                .isEqualTo(1L);
        assertThat(registry.get("agentic.node.duration").tag("node", "requirements").tag("outcome", "success").timer().count())
                .isEqualTo(1L);
    }
}
