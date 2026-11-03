package com.agentic.sdlc.controlplane.recovery;

import org.springframework.stereotype.Component;

@Component
public class RetryEvaluator {
    public boolean shouldRetry(RetryPolicy policy, int completedAttempt, FailureCategory category) {
        return completedAttempt < policy.maxAttempts()
                && policy.retryableCategories().contains(category);
    }
}
