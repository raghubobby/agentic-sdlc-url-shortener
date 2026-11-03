package com.agentic.sdlc.controlplane.llm;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ModelGatewayRouter implements ModelGateway {
    private final DeterministicModelGateway deterministic;
    private final OpenAiResponsesModelGateway openAi;
    private final ModelInvocationService invocationService;
    private final String provider;
    private final String configuredModel;

    public ModelGatewayRouter(
            DeterministicModelGateway deterministic,
            OpenAiResponsesModelGateway openAi,
            ModelInvocationService invocationService,
            @Value("${agentic.model.provider:deterministic}") String provider,
            @Value("${agentic.model.openai.model:}") String configuredModel) {
        this.deterministic = deterministic;
        this.openAi = openAi;
        this.invocationService = invocationService;
        this.provider = provider;
        this.configuredModel = configuredModel;
    }

    @Override
    public ModelResponse generate(ModelRequest request) {
        long started = System.nanoTime();
        try {
            ModelResponse response = switch (provider) {
                case "deterministic" -> deterministic.generate(request);
                case "openai-responses" -> openAi.generate(request);
                default -> throw new ModelGatewayException(
                        com.agentic.sdlc.controlplane.recovery.FailureCategory.VALIDATION,
                        "Unsupported model provider: " + provider);
            };
            invocationService.record(request, response);
            return response;
        } catch (RuntimeException ex) {
            invocationService.recordFailure(
                    request,
                    provider,
                    provider.equals("openai-responses") ? configuredModel : "fixture-v2",
                    Math.max(0L, (System.nanoTime() - started) / 1_000_000L),
                    ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage());
            throw ex;
        }
    }
}
