package com.agentic.sdlc.controlplane.tool;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactExecutionRendererTest {
    private final ArtifactExecutionRenderer renderer = new ArtifactExecutionRenderer();

    @Test
    void extractsReviewableDiffFromEngineeringArtifact() {
        String artifact = """
                reasoning
                --- BEGIN PRODUCED DIFF ---
                diff --git a/url-shortener/A.java b/url-shortener/A.java
                +hello
                --- END PRODUCED DIFF ---
                """;

        assertThat(renderer.extractProducedDiff(artifact))
                .contains("diff --git", "+hello");
    }
}
