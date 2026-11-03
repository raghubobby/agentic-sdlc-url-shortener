package com.agentic.sdlc.controlplane.governance;

import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;
import org.springframework.stereotype.Component;

@Component
public class PolicyEngine {
    private final RiskClassifier riskClassifier;

    public PolicyEngine(RiskClassifier riskClassifier) {
        this.riskClassifier = riskClassifier;
    }

    public PolicyOutcome evaluate(WorkflowNodeDefinition node) {
        RiskAssessment risk = riskClassifier.classify(node);

        if (node.impacts().contains(ChangeImpact.DIRECT_PRODUCTION_MUTATION)) {
            return new PolicyOutcome(
                    PolicyDecision.DENY, risk, 0,
                    "Agents are not permitted to directly mutate production");
        }

        return switch (risk.level()) {
            case LOW, MEDIUM -> new PolicyOutcome(
                    PolicyDecision.ALLOW, risk, 0,
                    "Risk is within autonomous execution boundary");
            case HIGH -> new PolicyOutcome(
                    PolicyDecision.REQUIRE_HUMAN_APPROVAL, risk, 1,
                    "High-risk change requires one human approval");
            case CRITICAL -> new PolicyOutcome(
                    PolicyDecision.REQUIRE_HUMAN_APPROVAL, risk, 2,
                    "Critical change requires two independent human approvals");
        };
    }
}
