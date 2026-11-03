package com.agentic.sdlc.controlplane.recovery;

import java.util.Set;

public record RetryPolicy(int maxAttempts, Set<FailureCategory> retryableCategories) {
    public RetryPolicy {
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("maxAttempts must be at least 1");
        }
        retryableCategories = Set.copyOf(retryableCategories);
    }

    public static RetryPolicy noRetry() {
        return new RetryPolicy(1, Set.of());
    }
}
