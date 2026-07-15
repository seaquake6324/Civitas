package com.seaquake6324.civitas.domain.civilization;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import org.junit.jupiter.api.Test;

class CivilityScoringRulesTest {
    @Test void scoresStayBoundedAndFacilityStacksSaturateAtThreeDistributionPoints() {
        CivilityScore three = CivilityScoringRules.score(evidence(List.of(3,3,3,3,3,3)));
        CivilityScore huge = CivilityScoringRules.score(evidence(List.of(99,99,99,99,99,99)));
        assertEquals(100, three.factors().facilities());
        assertEquals(three.factors().facilities(), huge.factors().facilities());
        assertTrue(three.factors().building() >= 0 && three.factors().building() <= 100);
        assertTrue(three.target() >= 0 && three.target() <= 100);
    }

    @Test void disconnectedCoreIsExplainableAndConnectivityIsCapped() {
        CivilityEvidence disconnected = new CivilityEvidence(100,64,64,64,1,64,0,0,
                List.of(1,1,1,1,0,0),4,2,2,true,false,BoundaryPorts.empty());
        CivilityScore score = CivilityScoringRules.score(disconnected);
        assertEquals(39, score.factors().connectivity());
        assertTrue(score.limits().contains(CivilityLimitReason.CORE_DISCONNECTED));
    }

    @Test void boundaryPortsRequireOppositeMatchingBits() {
        BoundaryPorts a = new BoundaryPorts(0, 4, 0, 0);
        assertTrue(a.connects(BoundaryPorts.Direction.EAST, new BoundaryPorts(0,0,0,4)));
        assertFalse(a.connects(BoundaryPorts.Direction.EAST, new BoundaryPorts(0,0,0,2)));
    }

    @Test void exposesStableDataDrivenTagPathsForAllSixFacilityCategories() {
        assertEquals(List.of("residential","production","storage","food","knowledge","public"),
                java.util.Arrays.stream(FacilityCategory.values()).map(FacilityCategory::tagPath).toList());
    }

    private static CivilityEvidence evidence(List<Integer> facilities) {
        return new CivilityEvidence(100,64,64,64,1,64,0,0,facilities,18,2,2,true,true,new BoundaryPorts(1,1,1,1));
    }
}
