package com.agentic.sdlc.controlplane.targetsystem;

import com.agentic.sdlc.controlplane.tool.WorkspaceManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

@Service
public class TargetSystemService {
    private final WorkspaceManager workspaceManager;
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String publicUrl;

    public TargetSystemService(
            WorkspaceManager workspaceManager,
            @Value("${agentic.target-system.base-url:http://localhost:8082}") String baseUrl,
            @Value("${agentic.target-system.public-url:http://localhost:8082}") String publicUrl) {
        this.workspaceManager = workspaceManager;
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.publicUrl = stripTrailingSlash(publicUrl);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    public TargetSystemSnapshot snapshot() {
        String root;
        String branch;
        String commit;

        try {
            root = workspaceManager.repositoryRoot().toString();
            branch = workspaceManager.currentBranch();
            commit = workspaceManager.shortHead();
        } catch (RuntimeException ex) {
            root = "unavailable";
            branch = "unavailable";
            commit = "unavailable";
        }

        String status = targetHealth();

        return new TargetSystemSnapshot(
                "URL Shortener",
                "Target application engineered by the agentic SDLC control plane",
                status,
                publicUrl,
                publicUrl + "/actuator/health",
                root,
                root.equals("unavailable") ? "url-shortener" : root + "/url-shortener",
                branch,
                commit,
                "url-shortener/**",
                "The control plane plans, changes, validates, and governs this URL-shortener codebase; it is not a separate assessment product."
        );
    }

    public ResponseEntity<String> createShortUrl(String requestJson, String idempotencyKey) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/v1/urls"))
                .timeout(Duration.ofSeconds(5))
                .header("Content-Type", "application/json")
                .header("Idempotency-Key", normalizeIdempotencyKey(idempotencyKey))
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return ResponseEntity.status(response.statusCode())
                    .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                    .body(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"detail\":\"URL shortener request was interrupted\"}");
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("{\"detail\":\"URL shortener target is not reachable on " + escapeJson(baseUrl) + "\"}");
        }
    }

    private String targetHealth() {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/actuator/health"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() >= 200 && response.statusCode() < 300 ? "UP" : "DEGRADED";
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            return "DOWN";
        } catch (Exception ex) {
            return "DOWN";
        }
    }

    private String normalizeIdempotencyKey(String value) {
        return value == null || value.isBlank() ? UUID.randomUUID().toString() : value;
    }

    private String stripTrailingSlash(String value) {
        String normalized = value == null || value.isBlank() ? "http://localhost:8082" : value.trim();
        while (normalized.endsWith("/")) normalized = normalized.substring(0, normalized.length() - 1);
        return normalized;
    }

    private String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
