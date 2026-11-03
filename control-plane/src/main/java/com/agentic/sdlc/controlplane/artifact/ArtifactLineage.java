package com.agentic.sdlc.controlplane.artifact;

import java.util.List;

public record ArtifactLineage(ArtifactView artifact, List<ArtifactInput> inputs) {
    public ArtifactLineage {
        inputs = List.copyOf(inputs);
    }

    public record ArtifactInput(
            String capturedInputHash,
            boolean hashMatchesArtifact,
            ArtifactLineage source
    ) {}
}
