package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.AgentCapability;
import com.agentic.sdlc.controlplane.agent.AgentRole;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tool_execution")
public class ToolExecutionEntity {
    @Id
    private UUID id;

    @Column(name = "workflow_id", nullable = false)
    private UUID workflowId;

    @Column(name = "node_key", nullable = false, length = 120)
    private String nodeKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "agent_role", nullable = false, length = 60)
    private AgentRole agentRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", nullable = false, length = 60)
    private ToolActionType toolType;

    @Enumerated(EnumType.STRING)
    @Column(name = "required_capability", nullable = false, length = 80)
    private AgentCapability requiredCapability;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ToolExecutionStatus status;

    @Column(name = "input_hash", nullable = false, length = 64)
    private String inputHash;

    @Column(name = "output_hash", length = 64)
    private String outputHash;

    @Column(name = "workspace_path", nullable = false, length = 1200)
    private String workspacePath;

    @Column(name = "exit_code")
    private Integer exitCode;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    protected ToolExecutionEntity() {}

    public ToolExecutionEntity(UUID id, ToolExecutionContext context, ToolAction action,
                               AgentCapability capability, String inputHash) {
        this.id = id;
        this.workflowId = context.workflowId();
        this.nodeKey = context.nodeKey();
        this.agentRole = context.agentRole();
        this.toolType = action.tool();
        this.requiredCapability = capability;
        this.status = ToolExecutionStatus.STARTED;
        this.inputHash = inputHash;
        this.workspacePath = context.workspace().toAbsolutePath().normalize().toString();
        this.startedAt = Instant.now();
    }

    public void succeed(ToolResult result, String outputHash, long latencyMs) {
        this.status = ToolExecutionStatus.SUCCEEDED;
        this.exitCode = result.exitCode();
        this.summary = result.summary();
        this.outputHash = outputHash;
        this.latencyMs = latencyMs;
        this.completedAt = Instant.now();
    }

    public void fail(ToolResult result, String outputHash, long latencyMs) {
        this.status = ToolExecutionStatus.FAILED;
        this.exitCode = result.exitCode();
        this.summary = result.summary();
        this.outputHash = outputHash;
        this.latencyMs = latencyMs;
        this.completedAt = Instant.now();
    }

    public void deny(String reason, long latencyMs) {
        this.status = ToolExecutionStatus.DENIED;
        this.summary = reason;
        this.latencyMs = latencyMs;
        this.completedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getWorkflowId() { return workflowId; }
    public String getNodeKey() { return nodeKey; }
    public AgentRole getAgentRole() { return agentRole; }
    public ToolActionType getToolType() { return toolType; }
    public AgentCapability getRequiredCapability() { return requiredCapability; }
    public ToolExecutionStatus getStatus() { return status; }
    public String getInputHash() { return inputHash; }
    public String getOutputHash() { return outputHash; }
    public String getWorkspacePath() { return workspacePath; }
    public Integer getExitCode() { return exitCode; }
    public String getSummary() { return summary; }
    public Long getLatencyMs() { return latencyMs; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
}
