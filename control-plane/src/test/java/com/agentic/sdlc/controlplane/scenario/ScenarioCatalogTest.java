package com.agentic.sdlc.controlplane.scenario;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ScenarioCatalogTest {
    private final ScenarioCatalog catalog = new ScenarioCatalog();

    @Test
    void exposesExactlyTheThreeAssessmentScenarios() {
        assertThat(catalog.all().stream().map(ScenarioBlueprint::type).toList())
                .containsExactly(ScenarioType.GREENFIELD, ScenarioType.BROWNFIELD, ScenarioType.AMBIGUOUS);
    }

    @Test
    void ambiguousScenarioMakesHumanClarificationPartOfAcceptanceCriteria() {
        ScenarioBlueprint scenario = catalog.get(ScenarioType.AMBIGUOUS);
        assertThat(scenario.requirement()).isEqualTo("Make shortened links more secure for enterprise customers.");
        assertThat(scenario.validationCriteria().toString()).contains("human clarification");
    }
}
