package com.agentic.sdlc.controlplane.clarification;

import com.agentic.sdlc.controlplane.workflow.WorkflowEngine;
import com.agentic.sdlc.controlplane.workflow.WorkflowSnapshot;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/workflows/{workflowId}/clarifications")
public class ClarificationController {
    private final ClarificationService service;
    private final WorkflowEngine engine;

    public ClarificationController(ClarificationService service, WorkflowEngine engine) {
        this.service = service;
        this.engine = engine;
    }

    @GetMapping
    public List<ClarificationView> history(@PathVariable UUID workflowId) {
        engine.snapshot(workflowId);
        return service.history(workflowId);
    }

    @PostMapping("/{clarificationId}/resolve")
    public ResolveResponse resolve(@PathVariable UUID workflowId,
                                   @PathVariable UUID clarificationId,
                                   @Valid @RequestBody ResolveRequest request) {
        ClarificationService.Resolution resolution = service.resolve(
                workflowId, clarificationId, request.answer(), request.resolvedBy());
        if (request.autoResume() == null || request.autoResume()) engine.runUntilBlocked(workflowId);
        return new ResolveResponse(resolution, engine.snapshot(workflowId));
    }

    public record ResolveRequest(@NotBlank String answer, @NotBlank String resolvedBy, Boolean autoResume) {}
    public record ResolveResponse(ClarificationService.Resolution resolution, WorkflowSnapshot workflow) {}
}
