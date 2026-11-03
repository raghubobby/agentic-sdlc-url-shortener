package com.agentic.sdlc.controlplane.execution;

import com.agentic.sdlc.controlplane.artifact.ArtifactType;
import com.agentic.sdlc.controlplane.workflow.WorkflowRunEntity;
import com.agentic.sdlc.controlplane.workflow.WorkflowRunRepository;

public class DeterministicNodeWorkExecutor implements NodeWorkExecutor {
    private final WorkflowRunRepository workflowRunRepository;

    public DeterministicNodeWorkExecutor(WorkflowRunRepository workflowRunRepository) {
        this.workflowRunRepository = workflowRunRepository;
    }

    @Override
    public NodeWorkResult execute(NodeExecutionContext context) {
        WorkflowRunEntity run = workflowRunRepository.findById(context.workflowId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow: " + context.workflowId()));

        ArtifactType artifactType = switch (context.definition().kind()) {
            case REQUIREMENT_ANALYSIS -> ArtifactType.REQUIREMENT_SPEC;
            case PLANNING -> ArtifactType.IMPLEMENTATION_PLAN;
            case ARCHITECTURE -> ArtifactType.ARCHITECTURE_DECISION;
            case IMPLEMENTATION -> ArtifactType.CODE_CHANGE;
            case VALIDATION -> context.definition().key().equals("release-readiness")
                    ? ArtifactType.RELEASE_READINESS : ArtifactType.TEST_EVIDENCE;
            case SECURITY_REVIEW -> ArtifactType.SECURITY_REPORT;
            case RELEASE -> ArtifactType.RELEASE_DECISION;
        };

        String content = """
                node=%s
                scenario=%s
                requirement=%s
                attempt=%d
                mode=deterministic-baseline
                """.formatted(
                context.definition().key(), run.getScenario(), run.getRequirement(), context.attempt()).trim();

        return NodeWorkResult.success(artifactType, content);
    }
}
