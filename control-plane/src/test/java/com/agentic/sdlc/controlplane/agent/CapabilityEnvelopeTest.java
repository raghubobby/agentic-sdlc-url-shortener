package com.agentic.sdlc.controlplane.agent;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CapabilityEnvelopeTest {
    @Test
    void rejectsModelRequestedCapabilityOutsideAgentAuthority() {
        CapabilityEnvelope envelope = new CapabilityEnvelope(
                EnumSet.of(AgentCapability.READ_REPOSITORY, AgentCapability.WRITE_ENGINEERING_ARTIFACT),
                EnumSet.of(AgentCapability.GIT_PUSH, AgentCapability.DEPLOY_PRODUCTION));

        assertThatThrownBy(() -> envelope.assertRequested(List.of("READ_REPOSITORY", "GIT_PUSH")))
                .isInstanceOf(CapabilityViolationException.class)
                .hasMessageContaining("GIT_PUSH");
    }

    @Test
    void treatsUnknownCapabilityAsDenied() {
        CapabilityEnvelope envelope = CapabilityEnvelope.readOnly();

        assertThatThrownBy(() -> envelope.assertRequested(List.of("BECOME_ADMIN")))
                .isInstanceOf(CapabilityViolationException.class)
                .hasMessageContaining("BECOME_ADMIN");
    }
}
