package com.agentic.sdlc.controlplane.llm;

public interface ModelGateway {
    ModelResponse generate(ModelRequest request);
}
