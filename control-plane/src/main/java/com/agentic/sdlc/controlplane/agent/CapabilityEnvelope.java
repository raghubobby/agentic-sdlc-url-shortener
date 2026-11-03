package com.agentic.sdlc.controlplane.agent;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;
import java.util.TreeSet;

public record CapabilityEnvelope(Set<AgentCapability> allowed, Set<AgentCapability> denied) {
    public CapabilityEnvelope {
        allowed = Set.copyOf(allowed);
        denied = Set.copyOf(denied);
        EnumSet<AgentCapability> overlap = EnumSet.noneOf(AgentCapability.class);
        overlap.addAll(allowed);
        overlap.retainAll(denied);
        if (!overlap.isEmpty()) {
            throw new IllegalArgumentException("Capabilities cannot be both allowed and denied: " + overlap);
        }
    }

    public void assertRequested(Collection<String> requestedCapabilities) {
        Set<String> violations = new TreeSet<>();
        for (String requested : requestedCapabilities) {
            try {
                AgentCapability capability = AgentCapability.valueOf(requested);
                if (!allowed.contains(capability) || denied.contains(capability)) violations.add(requested);
            } catch (IllegalArgumentException ex) {
                violations.add(requested);
            }
        }
        if (!violations.isEmpty()) {
            throw new CapabilityViolationException(violations);
        }
    }

    public static CapabilityEnvelope readOnly() {
        return new CapabilityEnvelope(
                EnumSet.of(AgentCapability.READ_REQUIREMENT, AgentCapability.READ_ARTIFACTS,
                        AgentCapability.READ_REPOSITORY, AgentCapability.WRITE_ENGINEERING_ARTIFACT),
                EnumSet.of(AgentCapability.WRITE_WORKTREE, AgentCapability.RUN_BUILD, AgentCapability.RUN_TESTS,
                        AgentCapability.RUN_SECURITY_SCAN, AgentCapability.GENERATE_DIFF, AgentCapability.RUN_SHELL,
                        AgentCapability.WRITE_MAIN, AgentCapability.GIT_PUSH, AgentCapability.DEPLOY_PRODUCTION));
    }
}
