package com.agentic.sdlc.controlplane.tool;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class WorkspaceManager {
    private final Path repositoryStart;
    private final Path workspaceRoot;
    private final ProcessExecutor processes;

    public WorkspaceManager(
            @Value("${agentic.tools.repository-root:.}") String repositoryRoot,
            @Value("${agentic.tools.workspace-root:}") String workspaceRoot,
            ProcessExecutor processes) {
        this.repositoryStart = Path.of(repositoryRoot).toAbsolutePath().normalize();
        this.workspaceRoot = (workspaceRoot == null || workspaceRoot.isBlank()
                ? Path.of(System.getProperty("java.io.tmpdir"), "agentic-workspaces")
                : Path.of(workspaceRoot)).toAbsolutePath().normalize();
        this.processes = processes;
    }

    public String currentHead() {
        Path root = repositoryRoot();
        ToolResult result = processes.run(root,
                List.of("git", "rev-parse", "HEAD"), Duration.ofSeconds(10));
        if (!result.success()) {
            throw new IllegalStateException("Repository root is not a readable Git checkout: " + root
                    + "\n" + result.output());
        }
        return result.output().trim();
    }

    public String shortHead() {
        Path root = repositoryRoot();
        ToolResult result = processes.run(root,
                List.of("git", "rev-parse", "--short", "HEAD"), Duration.ofSeconds(10));
        if (!result.success()) {
            throw new IllegalStateException("Could not read repository commit: " + result.output());
        }
        return result.output().trim();
    }

    public String currentBranch() {
        Path root = repositoryRoot();
        ToolResult result = processes.run(root,
                List.of("git", "branch", "--show-current"), Duration.ofSeconds(10));
        if (!result.success()) {
            throw new IllegalStateException("Could not read repository branch: " + result.output());
        }
        String branch = result.output().trim();
        return branch.isBlank() ? "detached" : branch;
    }

    public Path repositoryRoot() {
        ToolResult result = processes.run(repositoryStart,
                List.of("git", "rev-parse", "--show-toplevel"), Duration.ofSeconds(10));
        if (!result.success()) {
            throw new IllegalStateException("Configured repository path is not inside a Git checkout: "
                    + repositoryStart + "\n" + result.output());
        }
        return Path.of(result.output().trim()).toAbsolutePath().normalize();
    }

    public Path freshWorkspace(UUID workflowId, String nodeKey, int attempt, String baseCommit) {
        validateToken(nodeKey);
        Path root = repositoryRoot();
        String folder = nodeKey + "-a" + attempt;
        Path workflowDir = workspaceRoot.resolve(workflowId.toString()).normalize();
        Path target = workflowDir.resolve(folder).normalize();
        ensureUnderWorkspaceRoot(target);
        try {
            Files.createDirectories(workflowDir);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not create workflow workspace directory", ex);
        }

        if (Files.exists(target)) removeWorktree(root, target);
        ToolResult add = processes.run(root,
                List.of("git", "worktree", "add", "--detach", target.toString(), baseCommit),
                Duration.ofSeconds(30));
        if (!add.success()) {
            throw new IllegalStateException("Could not create isolated Git worktree: " + add.output());
        }
        return target;
    }

    public void discard(Path workspace) {
        if (workspace == null) return;
        Path normalized = workspace.toAbsolutePath().normalize();
        ensureUnderWorkspaceRoot(normalized);
        removeWorktree(repositoryRoot(), normalized);
    }

    private void removeWorktree(Path root, Path target) {
        ToolResult remove = processes.run(root,
                List.of("git", "worktree", "remove", "--force", target.toString()),
                Duration.ofSeconds(30));
        if (!remove.success() && Files.exists(target)) {
            try (var walk = Files.walk(target)) {
                walk.sorted(Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (IOException ex) { throw new IllegalStateException("Could not delete workspace " + path, ex); }
                });
            } catch (IOException ex) {
                throw new IllegalStateException("Could not clean workspace " + target, ex);
            }
        }
    }

    private void validateToken(String token) {
        if (token == null || !token.matches("[A-Za-z0-9._-]+")) {
            throw new IllegalArgumentException("Unsafe workspace token: " + token);
        }
    }

    private void ensureUnderWorkspaceRoot(Path candidate) {
        if (!candidate.startsWith(workspaceRoot)) {
            throw new IllegalArgumentException("Workspace path escapes configured root: " + candidate);
        }
    }
}
