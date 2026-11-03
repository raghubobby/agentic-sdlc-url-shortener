package com.agentic.sdlc.controlplane.llm;

import com.agentic.sdlc.controlplane.agent.AgentOutput;
import com.agentic.sdlc.controlplane.agent.AgentRole;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DeterministicModelGatewayTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DeterministicModelGateway gateway = new DeterministicModelGateway(objectMapper);

    @Test
    void emitsStructuredJsonThatBindsToAgentOutput() throws Exception {
        ModelResponse response = gateway.generate(request(
                "requirements",
                AgentRole.REQUIREMENTS,
                """
                Scenario: greenfield
                Workflow node: requirements
                Requirement:
                Add single-use short links.
                """));

        AgentOutput output = objectMapper.treeToValue(response.structuredOutput(), AgentOutput.class);

        assertThat(response.model()).isEqualTo("fixture-v3");
        assertThat(output.summary()).contains("single-use");
        assertThat(output.assumptions()).isNotEmpty();
        assertThat(output.ambiguities()).isEmpty();
        assertThat(output.requestedCapabilities()).isNotEmpty();
        assertThat(output.toolActions()).isEmpty();
    }

    @Test
    void ambiguousRequirementSafeStopsWithConcreteQuestions() throws Exception {
        ModelResponse response = gateway.generate(request(
                "requirements",
                AgentRole.REQUIREMENTS,
                """
                Scenario: ambiguous
                Workflow node: requirements
                Requirement:
                Make enterprise links more secure.
                """));

        AgentOutput output = objectMapper.treeToValue(response.structuredOutput(), AgentOutput.class);

        assertThat(output.ambiguities()).hasSize(3);
        assertThat(output.decisions()).anyMatch(value -> value.contains("Safe-stop"));
        assertThat(output.toolActions()).isEmpty();
    }

    @Test
    void greenfieldImplementationProducesGovernedPatchActions() throws Exception {
        ModelResponse response = gateway.generate(request(
                "implementation-api",
                AgentRole.IMPLEMENTER,
                """
                Scenario: greenfield
                Workflow node: implementation-api
                Requirement:
                Add single-use short links.
                """));

        AgentOutput output = objectMapper.treeToValue(response.structuredOutput(), AgentOutput.class);

        assertThat(output.toolActions()).hasSize(4);
        assertThat(output.toolActions().getFirst().content()).contains("singleUse");
        assertThat(output.toolActions().getFirst().content()).contains("PESSIMISTIC_WRITE");
    }

    @Test
    void brownfieldPersistenceRejectsUnnecessaryMutation() throws Exception {
        ModelResponse response = gateway.generate(request(
                "implementation-persistence",
                AgentRole.IMPLEMENTER,
                """
                Scenario: brownfield
                Workflow node: implementation-persistence
                Requirement:
                Prevent stale cache state from resurrecting inactive links.
                """));

        AgentOutput output = objectMapper.treeToValue(response.structuredOutput(), AgentOutput.class);

        assertThat(output.toolActions()).isEmpty();
        assertThat(output.decisions()).anyMatch(value -> value.contains("Reject an unnecessary schema change"));
    }

    private ModelRequest request(String nodeKey, AgentRole role, String task) {
        return new ModelRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                nodeKey,
                role,
                new ModelPrompt("test-system", task),
                "AgentOutput",
                Map.of());
    }
}
