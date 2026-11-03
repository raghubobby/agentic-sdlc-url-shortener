package com.agentic.sdlc.controlplane.targetsystem;

public record TargetSystemSnapshot(
        String name,
        String role,
        String status,
        String runtimeUrl,
        String healthUrl,
        String repositoryRoot,
        String targetModule,
        String branch,
        String commit,
        String changeScope,
        String note
) {}
