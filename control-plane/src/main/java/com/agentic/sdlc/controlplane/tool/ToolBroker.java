package com.agentic.sdlc.controlplane.tool;

import com.agentic.sdlc.controlplane.agent.CapabilityViolationException;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ToolBroker {
    private final Map<ToolActionType, EngineeringTool> tools;
    private final ToolPolicyInterceptor policy;
    private final ToolExecutionAuditService audit;

    public ToolBroker(List<EngineeringTool> tools, ToolPolicyInterceptor policy, ToolExecutionAuditService audit) {
        EnumMap<ToolActionType, EngineeringTool> map = new EnumMap<>(ToolActionType.class);
        for (EngineeringTool tool : tools) {
            if (map.put(tool.type(), tool) != null) {
                throw new IllegalStateException("Duplicate tool registration: " + tool.type());
            }
        }
        this.tools = Map.copyOf(map);
        this.policy = policy;
        this.audit = audit;
    }

    public java.util.List<ToolDescriptor> catalog() {
        return tools.entrySet().stream()
                .sorted(java.util.Map.Entry.comparingByKey())
                .map(entry -> new ToolDescriptor(entry.getKey(), entry.getValue().requiredCapability()))
                .toList();
    }

    public record ToolDescriptor(ToolActionType tool, com.agentic.sdlc.controlplane.agent.AgentCapability requiredCapability) {}

    public com.agentic.sdlc.controlplane.agent.AgentCapability requiredCapability(ToolActionType type) {
        EngineeringTool tool = tools.get(type);
        if (tool == null) throw new IllegalArgumentException("No registered tool for " + type);
        return tool.requiredCapability();
    }

    public ToolResult execute(ToolExecutionContext context, ToolAction action) {
        EngineeringTool tool = tools.get(action.tool());
        if (tool == null) throw new IllegalArgumentException("No registered tool for " + action.tool());

        UUID executionId = audit.started(context, action, tool.requiredCapability());
        long started = System.nanoTime();
        try {
            policy.authorize(context, tool.requiredCapability());
            ToolResult result = tool.execute(context, action);
            long latency = elapsedMillis(started);
            if (result.success()) audit.succeeded(executionId, result, latency);
            else audit.failed(executionId, result, latency);
            return result;
        } catch (CapabilityViolationException ex) {
            audit.denied(executionId, ex.getMessage(), elapsedMillis(started));
            throw ex;
        } catch (RuntimeException ex) {
            ToolResult failure = ToolResult.failure(-1,
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage(), "");
            audit.failed(executionId, failure, elapsedMillis(started));
            throw ex;
        }
    }

    private long elapsedMillis(long started) {
        return Math.max(0L, (System.nanoTime() - started) / 1_000_000L);
    }
}
