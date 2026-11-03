package com.agentic.sdlc.controlplane.api;

import com.agentic.sdlc.controlplane.agent.AgentRegistry;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agents")
public class AgentController {
    private final AgentRegistry agentRegistry;

    public AgentController(AgentRegistry agentRegistry) {
        this.agentRegistry = agentRegistry;
    }

    @GetMapping
    public List<AgentRegistry.AgentDescriptor> catalog() {
        return agentRegistry.catalog();
    }
}
