package com.agentic.sdlc.controlplane.execution;

import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;

import java.util.UUID;

public record NodeExecutionContext(
        UUID workflowId,
        WorkflowNodeDefinition definition,
        int attempt
) {}
