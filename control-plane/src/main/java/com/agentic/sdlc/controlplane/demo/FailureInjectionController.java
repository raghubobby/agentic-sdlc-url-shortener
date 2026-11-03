package com.agentic.sdlc.controlplane.demo;

import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@ConditionalOnProperty(name = "agentic.demo.failure-injection.enabled", havingValue = "true")
@RequestMapping("/api/demo/workflows/{workflowId}/failures")
public class FailureInjectionController {
    private final FailureInjectionService service;

    public FailureInjectionController(FailureInjectionService service) {
        this.service = service;
    }

    @PostMapping
    public FailureInjectionService.FailureInjectionView arm(
            @PathVariable UUID workflowId,
            @Valid @RequestBody ArmFailureRequest request) {
        return service.arm(workflowId, request.nodeKey(), request.category(), request.failures(),
                request.sideEffectsPossible(), request.message(), request.actor());
    }

    @GetMapping
    public List<FailureInjectionService.FailureInjectionView> list(@PathVariable UUID workflowId) {
        return service.list(workflowId);
    }

    @DeleteMapping("/{injectionId}")
    public FailureInjectionService.FailureInjectionView clear(
            @PathVariable UUID workflowId, @PathVariable UUID injectionId) {
        return service.clear(workflowId, injectionId);
    }

    public record ArmFailureRequest(
            @NotBlank String nodeKey,
            @NotNull FailureCategory category,
            @Min(1) @Max(5) int failures,
            boolean sideEffectsPossible,
            String message,
            String actor) {}
}
