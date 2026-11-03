package com.agentic.sdlc.controlplane.workflow;

import com.agentic.sdlc.controlplane.governance.ChangeImpact;
import com.agentic.sdlc.controlplane.governance.NodeKind;
import com.agentic.sdlc.controlplane.recovery.FailureCategory;
import com.agentic.sdlc.controlplane.recovery.RetryPolicy;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public final class WorkflowDefinition {
    private final Map<String, WorkflowNodeDefinition> nodes;

    public WorkflowDefinition(Collection<WorkflowNodeDefinition> nodes) {
        this.nodes = nodes.stream().collect(Collectors.toUnmodifiableMap(
                WorkflowNodeDefinition::key, Function.identity()));
        validate();
    }

    public static WorkflowDefinition defaultEngineeringWorkflow() {
        RetryPolicy transientRetry = new RetryPolicy(2, Set.of(
                FailureCategory.TRANSIENT_INFRASTRUCTURE, FailureCategory.RATE_LIMIT));
        RetryPolicy validationRetry = new RetryPolicy(3, Set.of(
                FailureCategory.TRANSIENT_INFRASTRUCTURE, FailureCategory.RATE_LIMIT));

        return new WorkflowDefinition(List.of(
                new WorkflowNodeDefinition(
                        "requirements", Set.of(), NodeKind.REQUIREMENT_ANALYSIS, Set.of(),
                        transientRetry, false),
                new WorkflowNodeDefinition(
                        "planning", Set.of("requirements"), NodeKind.PLANNING, Set.of(),
                        transientRetry, false),
                new WorkflowNodeDefinition(
                        "architecture", Set.of("planning"), NodeKind.ARCHITECTURE, Set.of(),
                        transientRetry, false),
                new WorkflowNodeDefinition(
                        "implementation-api", Set.of("architecture"), NodeKind.IMPLEMENTATION,
                        Set.of(ChangeImpact.API_CONTRACT), transientRetry, true),
                new WorkflowNodeDefinition(
                        "implementation-persistence", Set.of("architecture"), NodeKind.IMPLEMENTATION,
                        Set.of(ChangeImpact.DATA_MODEL), transientRetry, true),
                new WorkflowNodeDefinition(
                        "validation-tests", Set.of("implementation-api", "implementation-persistence"),
                        NodeKind.VALIDATION, Set.of(), validationRetry, false),
                new WorkflowNodeDefinition(
                        "security-review", Set.of("implementation-api", "implementation-persistence"),
                        NodeKind.SECURITY_REVIEW, Set.of(), transientRetry, false),
                new WorkflowNodeDefinition(
                        "release-approval", Set.of("validation-tests", "security-review"), NodeKind.RELEASE,
                        Set.of(ChangeImpact.RELEASE_ACTION), transientRetry, false),
                new WorkflowNodeDefinition(
                        "release-readiness", Set.of("release-approval"), NodeKind.VALIDATION, Set.of(),
                        transientRetry, false)
        ));
    }

    public Collection<WorkflowNodeDefinition> nodes() { return nodes.values(); }

    public WorkflowNodeDefinition node(String key) {
        WorkflowNodeDefinition node = nodes.get(key);
        if (node == null) throw new IllegalArgumentException("Unknown node: " + key);
        return node;
    }

    public Set<String> descendantsOf(String sourceKey) {
        node(sourceKey);
        Map<String, Set<String>> outgoing = new HashMap<>();
        nodes.keySet().forEach(key -> outgoing.put(key, new HashSet<>()));
        nodes.values().forEach(candidate -> candidate.dependencies().forEach(
                dependency -> outgoing.get(dependency).add(candidate.key())));

        Set<String> descendants = new TreeSet<>();
        Deque<String> queue = new ArrayDeque<>(outgoing.get(sourceKey));
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            if (descendants.add(current)) queue.addAll(outgoing.get(current));
        }
        return Collections.unmodifiableSet(descendants);
    }

    public List<WorkflowNodeDefinition> readyNodes(Map<String, NodeStatus> statuses) {
        return nodes.values().stream()
                .filter(node -> statuses.get(node.key()) == NodeStatus.PENDING)
                .filter(node -> node.dependencies().stream()
                        .allMatch(dep -> statuses.get(dep) == NodeStatus.SUCCEEDED))
                .sorted(Comparator.comparing(WorkflowNodeDefinition::key))
                .toList();
    }

    private void validate() {
        for (WorkflowNodeDefinition node : nodes.values()) {
            for (String dependency : node.dependencies()) {
                if (!nodes.containsKey(dependency)) {
                    throw new IllegalArgumentException(
                            "Node %s depends on unknown node %s".formatted(node.key(), dependency));
                }
                if (node.key().equals(dependency)) {
                    throw new IllegalArgumentException("Node cannot depend on itself: " + node.key());
                }
            }
        }

        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> outgoing = new HashMap<>();
        nodes.keySet().forEach(key -> { indegree.put(key, 0); outgoing.put(key, new ArrayList<>()); });
        nodes.values().forEach(node -> node.dependencies().forEach(dep -> {
            indegree.compute(node.key(), (k, v) -> v + 1);
            outgoing.get(dep).add(node.key());
        }));

        Deque<String> queue = new ArrayDeque<>();
        indegree.forEach((key, degree) -> { if (degree == 0) queue.add(key); });
        int visited = 0;
        while (!queue.isEmpty()) {
            String current = queue.removeFirst();
            visited++;
            for (String next : outgoing.get(current)) {
                int newDegree = indegree.compute(next, (k, v) -> v - 1);
                if (newDegree == 0) queue.addLast(next);
            }
        }
        if (visited != nodes.size()) throw new IllegalArgumentException("Workflow graph contains a cycle");
    }
}
