package com.agentic.sdlc.controlplane.llm;

import com.agentic.sdlc.controlplane.agent.AgentRole;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "model_invocation")
public class ModelInvocationEntity {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_key", nullable = false)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_role", nullable = false)
    private AgentRole agentRole;

    @Column(name = "provider", nullable = false)
    private String provider;

    @Column(name = "model_name", nullable = false)
    private String modelName;

    @Column(name = "prompt_hash", nullable = false, length = 64)
    private String promptHash;

    @Column(name = "response_hash", length = 64)
    private String responseHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ModelInvocationStatus status;

    @Column(name = "refusal_reason")
    private String refusalReason;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "latency_ms", nullable = false)
    private long latencyMs;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ModelInvocationEntity() {}

    public ModelInvocationEntity(
            UUID id, UUID workflowId, String nodeKey, AgentRole agentRole,
            String provider, String modelName, String promptHash, String responseHash,
            ModelInvocationStatus status, String refusalReason, String failureReason,
            long latencyMs, Integer inputTokens, Integer outputTokens) {
        this.id = id;
        this.workflowId = workflowId;
        this.nodeKey = nodeKey;
        this.agentRole = agentRole;
        this.provider = provider;
        this.modelName = modelName;
        this.promptHash = promptHash;
        this.responseHash = responseHash;
        this.status = status;
        this.refusalReason = refusalReason;
        this.failureReason = failureReason;
        this.latencyMs = latencyMs;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.createdAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getNodeKey() { return nodeKey; }
    public AgentRole getAgentRole() { return agentRole; }
    public String getProvider() { return provider; }
    public String getModelName() { return modelName; }
    public String getPromptHash() { return promptHash; }
    public String getResponseHash() { return responseHash; }
    public ModelInvocationStatus getStatus() { return status; }
    public String getRefusalReason() { return refusalReason; }
    public String getFailureReason() { return failureReason; }
    public long getLatencyMs() { return latencyMs; }
    public Integer getInputTokens() { return inputTokens; }
    public Integer getOutputTokens() { return outputTokens; }
    public Instant getCreatedAt() { return createdAt; }
}
