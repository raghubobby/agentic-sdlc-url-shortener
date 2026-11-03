package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.*;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.EnumSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ToolPolicyInterceptorTest {
    private final ToolPolicyInterceptor policy = new ToolPolicyInterceptor();

    @Test
    void independentlyDeniesToolCapabilityOutsideEnvelope() {
        CapabilityEnvelope envelope = new CapabilityEnvelope(
                EnumSet.of(AgentCapability.READ_REPOSITORY, AgentCapability.RUN_BUILD),
                EnumSet.of(AgentCapability.GIT_PUSH, AgentCapability.DEPLOY_PRODUCTION));
        ToolExecutionContext context = new ToolExecutionContext(
                UUID.randomUUID(), "implementation-api", 1, AgentRole.IMPLEMENTER,
                envelope, Path.of("."));

        policy.authorize(context, AgentCapability.RUN_BUILD);
        assertThatThrownBy(() -> policy.authorize(context, AgentCapability.GIT_PUSH))
                .isInstanceOf(CapabilityViolationException.class)
                .hasMessageContaining("GIT_PUSH");
    }
}
