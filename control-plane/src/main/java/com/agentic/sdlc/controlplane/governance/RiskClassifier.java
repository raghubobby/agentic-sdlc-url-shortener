package com.agentic.sdlc.controlplane.governance;

import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RiskClassifier {

    public RiskAssessment classify(WorkflowNodeDefinition node) {
        int score = baseScore(node.kind());
        List<String> reasons = new ArrayList<>();
        reasons.add("base:" + node.kind() + "=" + score);

        for (ChangeImpact impact : node.impacts()) {
            int weight = impactWeight(impact);
            score += weight;
            reasons.add("impact:" + impact + "+" + weight);
        }

        RiskLevel level = switch (score) {
            case 0 -> RiskLevel.LOW;
            case 1, 2 -> RiskLevel.MEDIUM;
            case 3, 4 -> RiskLevel.HIGH;
            default -> RiskLevel.CRITICAL;
        };
        return new RiskAssessment(level, score, reasons);
    }

    private int baseScore(NodeKind kind) {
        return switch (kind) {
            case REQUIREMENT_ANALYSIS, PLANNING, VALIDATION, SECURITY_REVIEW -> 0;
            case ARCHITECTURE, IMPLEMENTATION -> 1;
            case RELEASE -> 3;
        };
    }

    private int impactWeight(ChangeImpact impact) {
        return switch (impact) {
            case API_CONTRACT, EXTERNAL_INTEGRATION -> 1;
            case DATA_MODEL, SECURITY_SENSITIVE, RELEASE_ACTION -> 2;
            case DESTRUCTIVE -> 3;
            case DIRECT_PRODUCTION_MUTATION -> 5;
        };
    }
}
