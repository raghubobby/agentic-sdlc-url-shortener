package com.agentic.sdlc.controlplane.llm;

public record ModelPrompt(String system, String task) {
    public String render() {
        return system + "\n\n--- TASK ---\n" + task;
    }
}
