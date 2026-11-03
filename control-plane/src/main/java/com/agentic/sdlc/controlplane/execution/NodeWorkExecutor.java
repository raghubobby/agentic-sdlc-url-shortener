package com.agentic.sdlc.controlplane.execution;

public interface NodeWorkExecutor {
    NodeWorkResult execute(NodeExecutionContext context);

    default CompensationResult compensate(NodeExecutionContext context, NodeWorkResult failedResult) {
        return CompensationResult.succeeded("No external side effects required compensation");
    }
}
