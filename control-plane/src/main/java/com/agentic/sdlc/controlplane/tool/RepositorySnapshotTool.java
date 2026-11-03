package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Component
public class RepositorySnapshotTool implements EngineeringTool {
    private final ToolScopePolicy scope;

    public RepositorySnapshotTool(ToolScopePolicy scope) { this.scope = scope; }
    private static final int MAX_TOTAL_CHARS = 45_000;
    private static final int MAX_FILE_CHARS = 12_000;

    @Override public ToolActionType type() { return ToolActionType.READ_REPOSITORY_SNAPSHOT; }
    @Override public AgentCapability requiredCapability() { return AgentCapability.READ_REPOSITORY; }

    @Override
    public ToolResult execute(ToolExecutionContext context, ToolAction action) {
        String module = scope.productModule(action.module());
        Path root = safeResolve(context.workspace(), module);
        List<Path> files = new ArrayList<>();
        try (var walk = Files.walk(root)) {
            walk.filter(Files::isRegularFile)
                    .filter(this::eligible)
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(files::add);
        } catch (IOException ex) {
            throw new IllegalStateException("Could not read repository snapshot", ex);
        }

        StringBuilder snapshot = new StringBuilder();
        for (Path file : files) {
            if (snapshot.length() >= MAX_TOTAL_CHARS) break;
            String content;
            try { content = Files.readString(file, StandardCharsets.UTF_8); }
            catch (IOException | RuntimeException ex) { continue; }
            if (content.length() > MAX_FILE_CHARS) content = content.substring(0, MAX_FILE_CHARS) + "\n...[file truncated]";
            String relative = context.workspace().relativize(file).toString();
            String block = "\n--- " + relative + " ---\n" + content + "\n";
            int remaining = MAX_TOTAL_CHARS - snapshot.length();
            snapshot.append(block, 0, Math.min(block.length(), remaining));
        }
        if (snapshot.length() >= MAX_TOTAL_CHARS) snapshot.append("\n...[repository context budget reached]");
        return ToolResult.success("Repository snapshot captured from " + root.getFileName(), snapshot.toString());
    }

    private boolean eligible(Path path) {
        String value = path.toString();
        if (value.contains("/.git/") || value.contains("/target/") || value.contains("/.idea/")) return false;
        String name = path.getFileName().toString();
        return name.endsWith(".java") || name.endsWith(".xml") || name.endsWith(".yml") || name.endsWith(".yaml")
                || name.endsWith(".md") || name.equals("pom.xml");
    }

    private Path safeResolve(Path workspace, String module) {
        if (!module.matches("[A-Za-z0-9._/-]+")) throw new IllegalArgumentException("Unsafe repository module: " + module);
        Path resolved = workspace.resolve(module).normalize();
        if (!resolved.startsWith(workspace)) throw new IllegalArgumentException("Repository module escapes root");
        return resolved;
    }
}
