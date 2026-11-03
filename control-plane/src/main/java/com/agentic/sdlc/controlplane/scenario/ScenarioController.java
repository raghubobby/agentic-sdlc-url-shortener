package com.agentic.sdlc.controlplane.scenario;

import com.agentic.sdlc.controlplane.workflow.WorkflowEngine;
import com.agentic.sdlc.controlplane.workflow.WorkflowSnapshot;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/scenarios")
public class ScenarioController {
    private final ScenarioCatalog catalog;
    private final WorkflowEngine workflowEngine;

    public ScenarioController(ScenarioCatalog catalog, WorkflowEngine workflowEngine) {
        this.catalog = catalog;
        this.workflowEngine = workflowEngine;
    }

    @GetMapping
    public List<ScenarioBlueprint> scenarios() { return catalog.all(); }

    @PostMapping("/{scenario}/workflows")
    @ResponseStatus(HttpStatus.CREATED)
    public WorkflowSnapshot start(@PathVariable String scenario,
                                  @RequestBody(required = false) StartScenarioRequest request) {
        ScenarioBlueprint blueprint = catalog.get(scenario);
        String requirement = request != null && request.requirementOverride() != null
                && !request.requirementOverride().isBlank()
                ? request.requirementOverride().trim()
                : blueprint.requirement();
        UUID id = workflowEngine.create(blueprint.type().name().toLowerCase(), requirement);
        if (request == null || request.autoRun() == null || request.autoRun()) {
            workflowEngine.runUntilBlocked(id);
        }
        return workflowEngine.snapshot(id);
    }

    public record StartScenarioRequest(String requirementOverride, Boolean autoRun) {}
}
