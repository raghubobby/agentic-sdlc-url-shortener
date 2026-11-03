package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

@Component
public class PatchApplyTool implements EngineeringTool {
    private final ProcessExecutor processes;
    private final ToolScopePolicy scope;

    public PatchApplyTool(ProcessExecutor processes, ToolScopePolicy scope) {
        this.processes = processes;
        this.scope = scope;
    }

    @Override public ToolActionType type() { return ToolActionType.APPLY_PATCH; }
    @Override public AgentCapability requiredCapability() { return AgentCapability.WRITE_WORKTREE; }

    @Override
    public ToolResult execute(ToolExecutionContext context, ToolAction action) {
        scope.assertSafeProductPatch(action.content());
        Path patch;
        try {
            patch = Files.createTempFile(context.workspace(), ".agentic-", ".patch");
            Files.writeString(patch, action.content(), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not materialize proposed patch", ex);
        }
        try {
            ToolResult check = processes.run(context.workspace(),
                    List.of("git", "apply", "--check", patch.toString()), Duration.ofSeconds(20));
            if (!check.success()) {
                return ToolResult.failure(check.exitCode(), "Patch failed git apply --check", check.output());
            }
            return processes.run(context.workspace(),
                    List.of("git", "apply", "--index", "--whitespace=nowarn", patch.toString()), Duration.ofSeconds(20));
        } finally {
            try { Files.deleteIfExists(patch); } catch (IOException ignored) { }
        }
    }
}
