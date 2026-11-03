package com.agentic.sdlc.controlplane.agent;

import com.agentic.sdlc.controlplane.artifact.ArtifactView;
import com.agentic.sdlc.controlplane.llm.ModelPrompt;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.stream.Collectors;

@Component
public class AgentPromptFactory {
    private static final int MAX_ARTIFACT_CHARS = 12_000;

    public ModelPrompt build(AgentRole role, CapabilityEnvelope envelope, AgentExecutionContext context) {
        String upstream = context.upstreamArtifacts().stream()
                .sorted(Comparator.comparing(ArtifactView::nodeKey))
                .map(this::renderArtifact)
                .collect(Collectors.joining("\n\n"));
        if (upstream.isBlank()) upstream = "(none)";

        String system = """
                You are the %s agent inside a governed software-engineering control plane.
                Return only the requested structured engineering result. Do not claim that an action was executed
                unless the supplied evidence proves it. Never request capabilities outside the allowed set.
                Allowed capabilities: %s
                Explicitly denied capabilities: %s
                """.formatted(role, envelope.allowed(), envelope.denied()).trim();

        String task = """
                Scenario: %s
                Scenario execution guidance:
                %s

                Workflow node: %s
                Attempt: %d

                Requirement:
                %s

                Upstream engineering artifacts:
                %s

                Governed repository evidence:
                %s

                Produce a concise, defensible engineering output for this node. Identify assumptions, ambiguity,
                decisions, material risks, capabilities required for the proposed next work, and evidence used.

                Tool actions are proposals, not authority. Use only allow-listed structured tool actions and only when
                the node genuinely requires execution. The deterministic Tool Broker will independently authorize every
                action. Never request shell, Git push, main-branch writes, merge, or production deployment. All proposed
                file changes must stay under url-shortener/**; control-plane changes are outside agent authority. For an
                implementation node, when repository context is sufficient, propose a unified Git patch with repository-root
                relative paths followed by
                build, test, and diff actions. For validation/security nodes, request only the corresponding deterministic
                validation tools. Return an empty toolActions array when no tool execution is required.
                """.formatted(context.scenario(), scenarioGuidance(context.scenario()), context.node().key(), context.attempt(),
                context.requirement(), upstream, context.repositoryEvidence()).trim();

        return new ModelPrompt(system, task);
    }

    private String scenarioGuidance(String scenario) {
        return switch (scenario.toLowerCase()) {
            case "greenfield" -> "Decompose the new capability explicitly. Preserve backward compatibility and identify concurrency, schema, API, validation, and release implications before implementation.";
            case "brownfield" -> "Reason from the existing repository and evidence first. State the observed behavior, impacted code path, smallest safe change, regression risk, and validation strategy. Avoid rewrites without evidence.";
            case "ambiguous" -> "Treat unresolved material ambiguity as a safe-stop condition. Surface concrete clarification questions. Do not invent enterprise-security requirements or propose implementation until authoritative human clarification is present.";
            default -> "Apply production-grade software-engineering judgment and preserve evidence for every material decision.";
        };
    }

    private String renderArtifact(ArtifactView artifact) {
        String content = artifact.content();
        if (content.length() > MAX_ARTIFACT_CHARS) {
            content = content.substring(0, MAX_ARTIFACT_CHARS) + "\n...[truncated by context budget]";
        }
        return """
                [%s v%d hash=%s node=%s]
                %s
                """.formatted(artifact.type(), artifact.version(), artifact.contentHash(), artifact.nodeKey(), content).trim();
    }
}
