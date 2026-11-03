package com.agentic.sdlc.controlplane.tool;

public record ToolAction(
        ToolActionType tool,
        String path,
        String content,
        String module
) {
    public ToolAction {
        if (tool == null) throw new IllegalArgumentException("tool is required");
        path = blankToNull(path);
        content = blankToNull(content);
        module = blankToNull(module);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
