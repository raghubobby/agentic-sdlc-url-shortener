package com.agentic.sdlc.controlplane.audit;

import com.agentic.sdlc.controlplane.observability.ControlPlaneMetrics;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditEventRepository repository;
    private final ControlPlaneMetrics metrics;

    public AuditService(AuditEventRepository repository, ControlPlaneMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void append(UUID workflowId, String nodeKey, AuditEventType eventType, String payload) {
        repository.save(new AuditEventEntity(workflowId, nodeKey, eventType, payload));
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    metrics.recordAfterCommit(workflowId, nodeKey, eventType);
                }
            });
        }
    }

    @Transactional(readOnly = true)
    public List<AuditEventEntity> timeline(UUID workflowId) {
        return repository.findByWorkflowIdOrderByEventSequence(workflowId);
    }
}
