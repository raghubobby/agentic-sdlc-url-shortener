package com.agentic.sdlc.controlplane.governance;

import com.agentic.sdlc.controlplane.audit.AuditEventType;
import com.agentic.sdlc.controlplane.audit.AuditService;
import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GovernanceService {
    private final PolicyEngine policyEngine;
    private final PolicyEvaluationRepository repository;
    private final AuditService auditService;

    public GovernanceService(
            PolicyEngine policyEngine,
            PolicyEvaluationRepository repository,
            AuditService auditService) {
        this.policyEngine = policyEngine;
        this.repository = repository;
        this.auditService = auditService;
    }

    @Transactional
    public PolicyOutcome evaluate(UUID workflowId, WorkflowNodeDefinition node) {
        PolicyOutcome outcome = policyEngine.evaluate(node);
        repository.save(new PolicyEvaluationEntity(workflowId, node.key(), outcome));
        auditService.append(workflowId, node.key(), AuditEventType.POLICY_EVALUATED,
                "decision=%s;risk=%s;score=%d;requiredApprovals=%d;reason=%s".formatted(
                        outcome.decision(), outcome.risk().level(), outcome.risk().score(),
                        outcome.requiredApprovals(), outcome.reason()));
        return outcome;
    }

    @Transactional(readOnly = true)
    public List<PolicyEvaluationEntity> history(UUID workflowId) {
        return repository.findByWorkflowIdOrderByCreatedAtAsc(workflowId);
    }
}
