package com.agentic.sdlc.controlplane.workflow;

import com.agentic.sdlc.controlplane.governance.ChangeImpact;
import com.agentic.sdlc.controlplane.governance.NodeKind;
import com.agentic.sdlc.controlplane.recovery.RetryPolicy;

import java.util.Set;

public record WorkflowNodeDefinition(
        String key,
        Set<String> dependencies,
        NodeKind kind,
        Set<ChangeImpact> impacts,
        RetryPolicy retryPolicy,
        boolean compensatable
) {
    public WorkflowNodeDefinition {
        dependencies = Set.copyOf(dependencies);
        impacts = Set.copyOf(impacts);
    }
}
