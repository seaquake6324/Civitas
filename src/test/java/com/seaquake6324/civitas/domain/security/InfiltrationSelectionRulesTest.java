package com.seaquake6324.civitas.domain.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InfiltrationSelectionRulesTest {
    private final InfiltrationSelectionRules rules = new InfiltrationSelectionRules();
    private final InfiltrationSelectionRules.Weights weights = new InfiltrationSelectionRules.Weights(1, 1, .8, .75, .35, .5, .2, .3);

    @Test void preservesCompleteExplainableWeightsAndUsesDraw() {
        UUID city = UUID.randomUUID();
        var plan = rules.select(city, 4, List.of(
                new InfiltrationSelectionRules.Evidence(InfiltrationSource.WALL_GAP, 10, 1, 80, 10, 0, 0),
                new InfiltrationSelectionRules.Evidence(InfiltrationSource.DISGUISED_ENTRY, 20, 2, 80, 10, 90, 90)),
                weights, 0, 100, 2, 20, false, 16);
        assertEquals(InfiltrationSource.WALL_GAP, plan.selected().source());
        assertEquals(2, plan.candidates().size());
        assertEquals(100, plan.candidates().getFirst().unguarded());
        assertTrue(plan.candidates().getFirst().finalWeight() > plan.candidates().getLast().finalWeight());
    }

    @Test void capIsHardAndReportedAsTruncated() {
        var evidence = java.util.stream.IntStream.range(0, 20).mapToObj(i ->
                new InfiltrationSelectionRules.Evidence(InfiltrationSource.CAVE, i, i, 50, 80, 0, 0)).toList();
        var plan = rules.select(UUID.randomUUID(), 1, evidence, weights, .5, 1, 20, 80, false, 4);
        assertEquals(4, plan.candidates().size());
        assertTrue(plan.truncated());
    }

    @Test void rejectsEmptyCandidateSet() {
        assertThrows(IllegalArgumentException.class, () -> rules.select(UUID.randomUUID(), 1, List.of(), weights, 0, 0, 0, 0, false, 4));
    }
}
