package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.workflow.WorkflowNodeDefinition;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class AgentRegistry {
    private final List<EngineeringAgent> agents;

    public AgentRegistry(List<EngineeringAgent> agents) {
        this.agents = List.copyOf(agents);
    }

    public EngineeringAgent resolve(WorkflowNodeDefinition node) {
        List<EngineeringAgent> matches = agents.stream().filter(agent -> agent.supports(node)).toList();
        if (matches.size() != 1) {
            throw new IllegalStateException(
                    "Expected exactly one agent for node %s but found %d".formatted(node.key(), matches.size()));
        }
        return matches.getFirst();
    }

    public List<AgentDescriptor> catalog() {
        return agents.stream()
                .map(agent -> new AgentDescriptor(agent.role(), agent.capabilities()))
                .sorted(Comparator.comparing(descriptor -> descriptor.role().name()))
                .toList();
    }

    public record AgentDescriptor(AgentRole role, CapabilityEnvelope capabilities) {}
}
