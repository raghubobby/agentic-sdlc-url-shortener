package com.agentic.sdlc.controlplane.workflow;

public enum WorkflowStatus {
    CREATED,
    RUNNING,
    WAITING_FOR_APPROVAL,
    WAITING_FOR_CLARIFICATION,
    COMPLETED,
    FAILED
}
