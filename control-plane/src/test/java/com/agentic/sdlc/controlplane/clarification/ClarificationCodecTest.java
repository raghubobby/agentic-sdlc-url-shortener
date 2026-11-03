package com.agentic.sdlc.controlplane.clarification;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ClarificationCodecTest {
    @Test
    void roundTripsQuestionsWithoutLosingOrdering() {
        List<String> questions = List.of("Threat model?", "Authentication required?", "Compatibility constraints?");
        assertThat(ClarificationCodec.decode(ClarificationCodec.encode(questions))).isEqualTo(questions);
    }
}
