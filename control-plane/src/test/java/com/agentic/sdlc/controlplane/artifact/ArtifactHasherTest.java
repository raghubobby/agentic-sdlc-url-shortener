package com.agentic.sdlc.controlplane.artifact;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactHasherTest {
    @Test
    void producesStableSha256() {
        ArtifactHasher hasher = new ArtifactHasher();
        assertThat(hasher.sha256("governed-agentic-sdlc"))
                .isEqualTo("56e6274c64ea5989682bb26fae9f6ff8fb93e83d9e4b3ceac7a5ca2bdc24870b");
    }
}
