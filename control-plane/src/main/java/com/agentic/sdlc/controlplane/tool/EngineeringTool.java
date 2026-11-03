package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;

public interface EngineeringTool {
    ToolActionType type();
    AgentCapability requiredCapability();
    ToolResult execute(ToolExecutionContext context, ToolAction action);
}
