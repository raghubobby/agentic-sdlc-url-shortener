package com.agentic.sdlc.controlplane.tool;

public record ToolResult(
        boolean success,
        int exitCode,
        String summary,
        String output
) {
    public static ToolResult success(String summary, String output) {
        return new ToolResult(true, 0, summary, output == null ? "" : output);
    }

    public static ToolResult failure(int exitCode, String summary, String output) {
        return new ToolResult(false, exitCode, summary, output == null ? "" : output);
    }
}
