package com.agentic.sdlc.controlplane.targetsystem;

import com.agentic.sdlc.controlplane.tool.WorkspaceManager;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TargetSystemServiceTest {

    @Test
    void exposesUrlShortenerAsTheConcreteTargetEvenWhenRuntimeIsDown() {
        WorkspaceManager workspaceManager = mock(WorkspaceManager.class);
        when(workspaceManager.repositoryRoot()).thenReturn(Path.of("/tmp/assessment"));
        when(workspaceManager.currentBranch()).thenReturn("main");
        when(workspaceManager.shortHead()).thenReturn("abc1234");

        TargetSystemService service = new TargetSystemService(
                workspaceManager,
                "http://127.0.0.1:1",
                "http://localhost:8082");

        TargetSystemSnapshot snapshot = service.snapshot();

        assertThat(snapshot.name()).isEqualTo("URL Shortener");
        assertThat(snapshot.targetModule()).isEqualTo("/tmp/assessment/url-shortener");
        assertThat(snapshot.branch()).isEqualTo("main");
        assertThat(snapshot.commit()).isEqualTo("abc1234");
        assertThat(snapshot.changeScope()).isEqualTo("url-shortener/**");
        assertThat(snapshot.runtimeUrl()).isEqualTo("http://localhost:8082");
        assertThat(snapshot.status()).isEqualTo("DOWN");
    }
}
