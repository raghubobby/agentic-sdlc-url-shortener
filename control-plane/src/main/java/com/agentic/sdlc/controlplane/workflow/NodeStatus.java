package com.agentic.sdlc.controlplane.workflow;

public enum NodeStatus {
    PENDING,
    RUNNING,
    WAITING_FOR_APPROVAL,
    SUCCEEDED,
    FAILED
}
