package com.agentic.sdlc.controlplane.observability;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityController {
    private final WorkflowReliabilityService reliabilityService;

    public ObservabilityController(WorkflowReliabilityService reliabilityService) {
        this.reliabilityService = reliabilityService;
    }

    @GetMapping("/summary")
    public SystemReliabilitySnapshot summary() {
        return reliabilityService.systemSnapshot();
    }

    @GetMapping("/workflows/{workflowId}")
    public WorkflowReliabilitySnapshot workflow(@PathVariable UUID workflowId) {
        return reliabilityService.snapshot(workflowId);
    }
}
