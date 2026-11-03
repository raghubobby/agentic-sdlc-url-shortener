package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Component
public class TestRunnerTool implements EngineeringTool {
    private final ProcessExecutor processes;
    private final ToolScopePolicy scope;

    public TestRunnerTool(ProcessExecutor processes, ToolScopePolicy scope) {
        this.processes = processes;
        this.scope = scope;
    }

    @Override public ToolActionType type() { return ToolActionType.RUN_TESTS; }
    @Override public AgentCapability requiredCapability() { return AgentCapability.RUN_TESTS; }

    @Override
    public ToolResult execute(ToolExecutionContext context, ToolAction action) {
        String module = scope.productModule(action.module());
        return processes.run(context.workspace(),
                List.of("mvn", "-B", "-q", "-pl", module, "-am", "test"), Duration.ofMinutes(5));
    }
}
