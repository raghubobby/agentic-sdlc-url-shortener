package com.agentic.sdlc.controlplane.clarification;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ClarificationView(
        UUID id,
        UUID workflowId,
        int planRevision,
        String nodeKey,
        ClarificationStatus status,
        List<String> questions,
        String answer,
        Instant requestedAt,
        Instant resolvedAt,
        String resolvedBy
) {
    public static ClarificationView from(ClarificationRequestEntity entity) {
        return new ClarificationView(
                entity.getId(), entity.getWorkflowId(), entity.getPlanRevision(), entity.getNodeKey(),
                entity.getStatus(), ClarificationCodec.decode(entity.getQuestions()), entity.getAnswer(),
                entity.getRequestedAt(), entity.getResolvedAt(), entity.getResolvedBy());
    }
}
