package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import com.agentic.sdlc.controlplane.agent.CapabilityViolationException;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class ToolPolicyInterceptor {
    public void authorize(ToolExecutionContext context, AgentCapability required) {
        if (!context.capabilityEnvelope().allowed().contains(required)
                || context.capabilityEnvelope().denied().contains(required)) {
            throw new CapabilityViolationException(Set.of(required.name()));
        }
    }
}
