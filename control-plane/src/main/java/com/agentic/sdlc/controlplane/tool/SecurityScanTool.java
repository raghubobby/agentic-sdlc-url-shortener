package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class SecurityScanTool implements EngineeringTool {
    private final ToolScopePolicy scope;

    public SecurityScanTool(ToolScopePolicy scope) { this.scope = scope; }
    private static final long MAX_FILE_BYTES = 512_000;
    private static final Pattern PRIVATE_KEY = Pattern.compile("-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----");
    private static final Pattern AWS_ACCESS_KEY = Pattern.compile("AKIA[0-9A-Z]{16}");
    private static final Pattern HARDCODED_PASSWORD = Pattern.compile("(?i)(password|passwd|secret)\\s*[=:]\\s*[\\\"'][^\\\"']{6,}[\\\"']");
    private static final Pattern DANGEROUS_PROCESS = Pattern.compile("Runtime\\.getRuntime\\(\\)\\.exec|new\\s+ProcessBuilder\\s*\\(");

    @Override public ToolActionType type() { return ToolActionType.RUN_SECURITY_SCAN; }
    @Override public AgentCapability requiredCapability() { return AgentCapability.RUN_SECURITY_SCAN; }

    @Override
    public ToolResult execute(ToolExecutionContext context, ToolAction action) {
        String module = scope.productModule(action.module());
        Path moduleRoot = safeResolve(context.workspace(), module);
        List<String> findings = new ArrayList<>();
        try (var paths = Files.walk(moduleRoot)) {
            paths.filter(Files::isRegularFile)
                    .filter(this::isCandidate)
                    .forEach(path -> scan(path, findings));
        } catch (IOException ex) {
            throw new IllegalStateException("Security scan could not traverse workspace", ex);
        }
        if (findings.isEmpty()) return ToolResult.success("Static security scan passed", "No high-signal findings");
        String output = String.join("\n", findings);
        return ToolResult.failure(3, "Static security scan found " + findings.size() + " issue(s)", output);
    }

    private boolean isCandidate(Path path) {
        String value = path.toString();
        if (value.contains("/.git/") || value.contains("/target/")) return false;
        try { return Files.size(path) <= MAX_FILE_BYTES; }
        catch (IOException ex) { return false; }
    }

    private void scan(Path path, List<String> findings) {
        String content;
        try { content = Files.readString(path, StandardCharsets.UTF_8); }
        catch (IOException | RuntimeException ex) { return; }
        match(PRIVATE_KEY, content, path, "embedded private key", findings);
        match(AWS_ACCESS_KEY, content, path, "AWS access key pattern", findings);
        match(HARDCODED_PASSWORD, content, path, "possible hardcoded credential", findings);
        match(DANGEROUS_PROCESS, content, path, "process execution requires review", findings);
    }

    private void match(Pattern pattern, String content, Path path, String message, List<String> findings) {
        if (pattern.matcher(content).find()) findings.add(path + ": " + message);
    }

    private Path safeResolve(Path workspace, String module) {
        if (!module.matches("[A-Za-z0-9._/-]+")) throw new IllegalArgumentException("Unsafe scan module: " + module);
        Path resolved = workspace.resolve(module).normalize();
        if (!resolved.startsWith(workspace)) throw new IllegalArgumentException("Scan module escapes workspace");
        return resolved;
    }
}
