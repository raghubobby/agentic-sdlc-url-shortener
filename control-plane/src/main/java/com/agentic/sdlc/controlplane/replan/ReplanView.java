package com.agentic.sdlc.controlplane.replan;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ReplanView(
        UUID id,
        UUID workflowId,
        String changeId,
        int fromRevision,
        int toRevision,
        String sourceNodeKey,
        String previousHash,
        String newHash,
        boolean applied,
        List<String> lineageImpactedNodes,
        List<String> invalidatedNodes,
        List<String> preservedNodes,
        List<String> nextReadyNodes,
        String reason,
        String changedBy,
        Instant createdAt
) {
    public static ReplanView from(ReplanRecordEntity entity) {
        return new ReplanView(
                entity.getId(), entity.getWorkflowId(), entity.getChangeId(),
                entity.getFromRevision(), entity.getToRevision(), entity.getSourceNodeKey(),
                entity.getPreviousHash(), entity.getNewHash(), entity.isApplied(),
                decode(entity.getLineageImpactedNodes()), decode(entity.getInvalidatedNodes()), decode(entity.getPreservedNodes()),
                decode(entity.getNextReadyNodes()), entity.getReason(), entity.getChangedBy(), entity.getCreatedAt());
    }

    static String encode(Iterable<String> values) {
        java.util.Set<String> sorted = new java.util.TreeSet<>();
        values.forEach(sorted::add);
        return String.join(",", sorted);
    }

    static List<String> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) return List.of();
        return java.util.Arrays.stream(encoded.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .sorted()
                .toList();
    }
}
