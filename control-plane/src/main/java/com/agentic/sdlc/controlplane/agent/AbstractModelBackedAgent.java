package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.artifact.ArtifactType;
import com.agentic.sdlc.controlplane.llm.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractModelBackedAgent implements EngineeringAgent {
    private final ModelGateway modelGateway;
    private final AgentPromptFactory promptFactory;
    private final ObjectMapper objectMapper;
    private final AgentRole role;
    private final CapabilityEnvelope capabilities;

    protected AbstractModelBackedAgent(
            ModelGateway modelGateway,
            AgentPromptFactory promptFactory,
            ObjectMapper objectMapper,
            AgentRole role,
            CapabilityEnvelope capabilities) {
        this.modelGateway = modelGateway;
        this.promptFactory = promptFactory;
        this.objectMapper = objectMapper;
        this.role = role;
        this.capabilities = capabilities;
    }

    @Override
    public AgentRole role() {
        return role;
    }

    @Override
    public CapabilityEnvelope capabilities() {
        return capabilities;
    }

    @Override
    public AgentResult execute(AgentExecutionContext context) {
        ModelPrompt prompt = promptFactory.build(role, capabilities, context);
        ModelRequest request = new ModelRequest(
                UUID.randomUUID(), context.workflowId(), context.node().key(), role,
                prompt, role.name().toLowerCase() + "_engineering_output", AgentOutputSchema.jsonSchema());
        ModelResponse response = modelGateway.generate(request);
        if (response.refused()) {
            throw new ModelRefusalException(response.refusalReason() == null
                    ? "Model refused the engineering request"
                    : response.refusalReason());
        }
        if (response.structuredOutput() == null) {
            throw new ModelGatewayException(
                    com.agentic.sdlc.controlplane.recovery.FailureCategory.VALIDATION,
                    "Model did not return structured output");
        }

        try {
            AgentOutput output = objectMapper.treeToValue(response.structuredOutput(), AgentOutput.class);
            capabilities.assertRequested(output.requestedCapabilities());
            ArtifactType artifactType = artifactType(context);
            String content = renderArtifact(context, response, output);
            return new AgentResult(role, response.provider(), response.model(), output, artifactType, content);
        } catch (JacksonException ex) {
            throw new ModelGatewayException(
                    com.agentic.sdlc.controlplane.recovery.FailureCategory.VALIDATION,
                    "Structured model output could not be bound to AgentOutput", ex);
        }
    }

    protected abstract ArtifactType artifactType(AgentExecutionContext context);

    private String renderArtifact(AgentExecutionContext context, ModelResponse response, AgentOutput output)
            throws JacksonException {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("node", context.node().key());
        payload.put("agentRole", role.name());
        payload.put("provider", response.provider());
        payload.put("model", response.model());
        payload.put("attempt", context.attempt());
        payload.put("summary", output.summary());
        payload.put("confidence", output.confidence());
        payload.put("assumptions", output.assumptions());
        payload.put("ambiguities", output.ambiguities());
        payload.put("decisions", output.decisions());
        payload.put("risks", output.risks());
        payload.put("requestedCapabilities", output.requestedCapabilities());
        payload.put("evidence", output.evidence());
        payload.put("toolActions", output.toolActions());
        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(payload);
    }
}
