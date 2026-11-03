package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.artifact.ArtifactView;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class WorkspaceAssemblyService {
    private final ArtifactExecutionRenderer renderer;
    private final ProcessExecutor processes;
    private final WorkspaceAuditService audit;

    public WorkspaceAssemblyService(
            ArtifactExecutionRenderer renderer,
            ProcessExecutor processes,
            WorkspaceAuditService audit) {
        this.renderer = renderer;
        this.processes = processes;
        this.audit = audit;
    }

    public int applyUpstreamChanges(UUID workflowId, String nodeKey, Path workspace, List<ArtifactView> upstream) {
        int applied = 0;
        for (ArtifactView artifact : upstream.stream().sorted(Comparator.comparing(ArtifactView::nodeKey)).toList()) {
            String diff = renderer.extractProducedDiff(artifact.content());
            if (diff.isBlank()) continue;
            applyPlatformPatch(workspace, diff, artifact.nodeKey());
            applied++;
        }
        audit.assembled(workflowId, nodeKey, applied);
        return applied;
    }

    private void applyPlatformPatch(Path workspace, String diff, String sourceNode) {
        Path patch;
        try {
            patch = Files.createTempFile(workspace, ".upstream-", ".patch");
            Files.writeString(patch, diff, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not materialize upstream patch from " + sourceNode, ex);
        }
        try {
            ToolResult check = processes.run(workspace,
                    List.of("git", "apply", "--check", patch.toString()), Duration.ofSeconds(20));
            if (!check.success()) {
                throw new IllegalStateException("Upstream patch from " + sourceNode + " conflicts: " + check.output());
            }
            ToolResult apply = processes.run(workspace,
                    List.of("git", "apply", "--index", "--whitespace=nowarn", patch.toString()), Duration.ofSeconds(20));
            if (!apply.success()) {
                throw new IllegalStateException("Could not apply upstream patch from " + sourceNode + ": " + apply.output());
            }
        } finally {
            try { Files.deleteIfExists(patch); } catch (IOException ignored) { }
        }
    }
}
