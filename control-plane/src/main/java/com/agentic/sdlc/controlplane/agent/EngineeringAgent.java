package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;

public interface EngineeringAgent {
    AgentRole role();
    boolean supports(WorkflowNodeDefinition node);
    CapabilityEnvelope capabilities();
    AgentResult execute(AgentExecutionContext context);
}
