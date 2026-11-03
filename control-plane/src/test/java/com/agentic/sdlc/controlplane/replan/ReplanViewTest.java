package com.agentic.sdlc.controlplane.replan;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ReplanViewTest {
    @Test
    void encodesNodeSetsDeterministicallyForAuditStorage() {
        String encoded = ReplanView.encode(List.of("release-readiness", "validation-tests"));

        // Audit storage uses lexicographic ordering so the same logical set
        // always produces the same persisted representation/hash.
        assertThat(encoded).isEqualTo("release-readiness,validation-tests");
        assertThat(ReplanView.decode(encoded))
                .containsExactly("release-readiness", "validation-tests");
    }
}
