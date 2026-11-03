package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class DiffTool implements EngineeringTool {
    private final ProcessExecutor processes;

    public DiffTool(ProcessExecutor processes) { this.processes = processes; }

    @Override public ToolActionType type() { return ToolActionType.GENERATE_DIFF; }
    @Override public AgentCapability requiredCapability() { return AgentCapability.GENERATE_DIFF; }

    @Override
    public ToolResult execute(ToolExecutionContext context, ToolAction action) {
        return processes.run(context.workspace(),
                List.of("git", "diff", "--cached", "--no-ext-diff", "--binary", "--", "."), Duration.ofSeconds(30));
    }
}
