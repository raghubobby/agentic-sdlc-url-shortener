package com.agentic.sdlc.controlplane.targetsystem;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/target-system")
public class TargetSystemController {
    private final TargetSystemService targetSystemService;

    public TargetSystemController(TargetSystemService targetSystemService) {
        this.targetSystemService = targetSystemService;
    }

    @GetMapping
    TargetSystemSnapshot targetSystem() {
        return targetSystemService.snapshot();
    }

    @PostMapping("/urls")
    ResponseEntity<String> createShortUrl(
            @RequestBody String requestJson,
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey) {
        return targetSystemService.createShortUrl(requestJson, idempotencyKey);
    }
}
