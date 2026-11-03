package com.agentic.sdlc.controlplane.demo;

import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FailureInjectionEntityTest {
    @Test
    void consumesBoundedFailuresAndDeactivates() {
        FailureInjectionEntity entity = new FailureInjectionEntity(
                UUID.randomUUID(), "requirements", FailureCategory.TRANSIENT_INFRASTRUCTURE,
                2, false, "demo", "tester");

        entity.consume();
        assertThat(entity.getRemainingFailures()).isEqualTo(1);
        assertThat(entity.isActive()).isTrue();

        entity.consume();
        assertThat(entity.getRemainingFailures()).isZero();
        assertThat(entity.isActive()).isFalse();
        assertThatThrownBy(entity::consume).isInstanceOf(IllegalStateException.class);
    }
}
