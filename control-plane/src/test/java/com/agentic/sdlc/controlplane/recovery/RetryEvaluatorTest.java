package com.agentic.sdlc.controlplane.recovery;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RetryEvaluatorTest {
    private final RetryEvaluator evaluator = new RetryEvaluator();
    private final RetryPolicy policy = new RetryPolicy(
            3, Set.of(FailureCategory.TRANSIENT_INFRASTRUCTURE, FailureCategory.RATE_LIMIT));

    @Test
    void retriesOnlyBoundedRetryableFailures() {
        assertThat(evaluator.shouldRetry(policy, 1, FailureCategory.TRANSIENT_INFRASTRUCTURE)).isTrue();
        assertThat(evaluator.shouldRetry(policy, 2, FailureCategory.RATE_LIMIT)).isTrue();
        assertThat(evaluator.shouldRetry(policy, 3, FailureCategory.TRANSIENT_INFRASTRUCTURE)).isFalse();
        assertThat(evaluator.shouldRetry(policy, 1, FailureCategory.VALIDATION)).isFalse();
    }
}
