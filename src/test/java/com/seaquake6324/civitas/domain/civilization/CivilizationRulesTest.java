package com.seaquake6324.civitas.domain.civilization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

class CivilizationRulesTest {
    @Test
    void computesWeightedTargetAndAppliesLowFactorCaps() {
        assertEquals(100, new CivilizationFactors(100, 100, 100, 100).target());
        assertEquals(39, new CivilizationFactors(19, 100, 100, 100).target());
        assertEquals(59, new CivilizationFactors(100, 19, 100, 100).target());
        assertEquals(39, new CivilizationFactors(100, 100, 100, 19).target());
    }

    @Test
    void interpolatesSuppressionAndLetsActivityOnlyModifyCivility() {
        SpawnSuppressionCurve curve = new SpawnSuppressionCurve(List.of(
                new SpawnSuppressionCurve.Point(0, 0), new SpawnSuppressionCurve.Point(20, .10),
                new SpawnSuppressionCurve.Point(40, .30), new SpawnSuppressionCurve.Point(100, .90)));
        assertEquals(.20, curve.base(30), 0.0001);
        assertEquals(0, curve.effective(0, 100), 0.0001);
        assertEquals(.08, curve.effective(30, 0), 0.0001);
        assertEquals(.20, curve.effective(30, 100), 0.0001);

        SpawnSuppressionCurve.Breakdown breakdown = curve.breakdown(30, 0);
        assertEquals(.20, breakdown.baseSuppression(), 0.0001);
        assertEquals(.40, breakdown.activityModifier(), 0.0001);
        assertEquals(.08, breakdown.finalSuppression(), 0.0001);
        assertEquals(breakdown.finalSuppression(), curve.effective(30, 0), 0.0001);
        assertEquals(1.0, curve.breakdown(30, 100).activityModifier(), 0.0001);
    }

    @Test
    void keepsLayerValuesIndependent() {
        ChunkCivilization surface = ChunkCivilization.empty().withActivity(80, 1);
        ChunkCivilization underground = ChunkCivilization.empty();
        assertEquals(ActivityTier.THRIVING, surface.activityTier());
        assertEquals(ActivityTier.DESERTED, underground.activityTier());
    }
}
