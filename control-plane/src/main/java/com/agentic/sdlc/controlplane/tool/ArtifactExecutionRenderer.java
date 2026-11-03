package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentResult;
import org.springframework.stereotype.Component;

import java.util.HexFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.nio.charset.StandardCharsets;

@Component
public class ArtifactExecutionRenderer {
    private static final String DIFF_BEGIN = "--- BEGIN PRODUCED DIFF ---";
    private static final String DIFF_END = "--- END PRODUCED DIFF ---";

    public String render(AgentResult result, ToolExecutionSummary execution) {
        StringBuilder out = new StringBuilder(result.artifactContent());
        if (!execution.hasExecution()) return out.toString();
        out.append("\n\n--- BEGIN TOOL EVIDENCE ---\n");
        for (ToolExecutionSummary.Step step : execution.steps()) {
            out.append("tool=").append(step.tool())
                    .append(";exitCode=").append(step.exitCode())
                    .append(";outputHash=").append(step.outputHash())
                    .append(";summary=").append(step.summary().replace('\n', ' '))
                    .append('\n');
        }
        out.append("--- END TOOL EVIDENCE ---\n");
        if (!execution.producedDiff().isBlank()) {
            out.append('\n').append(DIFF_BEGIN).append('\n')
                    .append(execution.producedDiff())
                    .append(execution.producedDiff().endsWith("\n") ? "" : "\n")
                    .append(DIFF_END).append('\n');
        }
        return out.toString();
    }

    public String extractProducedDiff(String artifactContent) {
        int start = artifactContent.indexOf(DIFF_BEGIN);
        if (start < 0) return "";
        start += DIFF_BEGIN.length();
        int end = artifactContent.indexOf(DIFF_END, start);
        if (end < 0) throw new IllegalArgumentException("Artifact contains unterminated produced diff section");
        return artifactContent.substring(start, end).stripLeading();
    }

    public String hash(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
