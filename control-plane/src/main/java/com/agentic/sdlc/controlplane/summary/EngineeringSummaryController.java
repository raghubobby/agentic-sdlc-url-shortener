package com.agentic.sdlc.controlplane.summary;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/workflows/{workflowId}/engineering-summary")
public class EngineeringSummaryController {
    private final EngineeringSummaryService service;

    public EngineeringSummaryController(EngineeringSummaryService service) { this.service = service; }

    @GetMapping
    public EngineeringSummary summary(@PathVariable UUID workflowId) { return service.build(workflowId); }
}
