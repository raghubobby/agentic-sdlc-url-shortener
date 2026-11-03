package com.agentic.sdlc.controlplane.tool;

import java.util.List;

public record ToolExecutionSummary(
        List<Step> steps,
        String producedDiff
) {
    public ToolExecutionSummary {
        steps = List.copyOf(steps);
        producedDiff = producedDiff == null ? "" : producedDiff;
    }

    public boolean hasExecution() { return !steps.isEmpty(); }

    public record Step(ToolActionType tool, int exitCode, String summary, String outputHash) {}

    public static ToolExecutionSummary none() { return new ToolExecutionSummary(List.of(), ""); }
}
