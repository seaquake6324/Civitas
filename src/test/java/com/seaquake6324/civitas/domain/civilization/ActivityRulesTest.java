package com.seaquake6324.civitas.domain.civilization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActivityRulesTest {
    @Test void repeatedCategoryIsDeduplicated() {
        ActivityWindowEvidence evidence = ActivityWindowEvidence.empty(0)
                .record(ActivityCategory.CONSTRUCTION, UUID.randomUUID())
                .record(ActivityCategory.CONSTRUCTION, UUID.randomUUID());
        assertEquals(1, evidence.categoryCount());
        assertEquals(2, evidence.contributorCount());
    }

    @Test void oneThroughFiveCategoriesHaveExactSinglePlayerGains() {
        assertEquals(.5, ActivityRules.directGain(1, 1, .5, 3.5));
        assertEquals(1, ActivityRules.directGain(2, 1, .5, 3.5));
        assertEquals(2, ActivityRules.directGain(3, 1, .5, 3.5));
        assertEquals(2.5, ActivityRules.directGain(4, 1, .5, 3.5));
        assertEquals(3.5, ActivityRules.directGain(5, 1, .5, 3.5));
    }

    @Test void contributorModifierIsDiminishingAndCappedAtFour() {
        assertEquals(2, ActivityRules.directGain(3, 1, .5, 3.5));
        assertEquals(2.4, ActivityRules.directGain(3, 2, .5, 3.5), 1e-9);
        assertEquals(2.8, ActivityRules.directGain(3, 3, .5, 3.5), 1e-9);
        assertEquals(3.2, ActivityRules.directGain(3, 4, .5, 3.5), 1e-9);
        assertEquals(3.2, ActivityRules.directGain(3, 20, .5, 3.5), 1e-9);
    }

    @Test void gainsAndActivityAreCapped() {
        assertEquals(3.5, ActivityRules.directGain(5, 4, .5, 3.5));
        assertEquals(3.5, ActivityRules.combinedGain(3, 1, 3.5));
        assertEquals(0, ActivityRules.clamp(-1));
        assertEquals(100, ActivityRules.clamp(101));
    }

    @Test void graceAndFullDecayPeriodsUseExactBoundaries() {
        ActivitySummary summary = ActivitySummary.empty().settled(100, 1, .5, 0, 1);
        assertEquals(10, ActivityRules.decay(10, summary, 6099, 6000, 1200, .25).activity());
        assertEquals(10, ActivityRules.decay(10, summary, 6100, 6000, 1200, .25).activity());
        assertEquals(10, ActivityRules.decay(10, summary, 7299, 6000, 1200, .25).activity());
        assertEquals(9.75, ActivityRules.decay(10, summary, 7300, 6000, 1200, .25).activity());
        assertEquals(7.5, ActivityRules.decay(10, summary, 18100, 6000, 1200, .25).activity());
    }

    @Test void propagationRatioAndAggregateCapAreBounded() {
        assertEquals(.7, ActivityRules.propagatedGain(2, .35), 1e-9);
        assertEquals(1, ActivityRules.capPropagated(.7, .7, 1), 1e-9);
    }

    @Test void contributorEvidenceStopsRetainingUuidsAtSixteen() {
        ActivityWindowEvidence evidence = ActivityWindowEvidence.empty(0);
        for (int i = 0; i < 20; i++) evidence = evidence.record(ActivityCategory.TRANSIT, UUID.randomUUID());
        assertEquals(16, evidence.contributors().size());
        assertTrue(evidence.contributorOverflow());
        assertEquals(17, evidence.contributorCount());
    }

    @Test void layersRemainDistinctDomainKeys() {
        assertFalse(CivilizationLayer.SURFACE == CivilizationLayer.UNDERGROUND);
        assertTrue(ActivityCategory.TRANSIT.bit() != ActivityCategory.CONSTRUCTION.bit());
    }
}
