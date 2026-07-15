package com.seaquake6324.civitas.domain.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NaturalDespawnRulesTest {
    @Test
    void countsOnlyFromInclusiveThirtyTwoThroughInclusiveOneHundredTwentyEightBlocks() {
        assertEquals(32, NaturalDespawnRules.MINIMUM_DISTANCE);
        assertEquals(NaturalDespawnRules.Decision.RESET,
                NaturalDespawnRules.evaluate(eligible(NaturalDespawnRules.MINIMUM_DISTANCE_SQUARED - 0.01)));
        assertEquals(NaturalDespawnRules.Decision.COUNT,
                NaturalDespawnRules.evaluate(eligible(NaturalDespawnRules.MINIMUM_DISTANCE_SQUARED)));
        assertEquals(NaturalDespawnRules.Decision.COUNT,
                NaturalDespawnRules.evaluate(eligible(NaturalDespawnRules.VANILLA_INSTANT_DISTANCE_SQUARED)));
        assertEquals(NaturalDespawnRules.Decision.DEFER_TO_VANILLA,
                NaturalDespawnRules.evaluate(eligible(NaturalDespawnRules.VANILLA_INSTANT_DISTANCE_SQUARED + 0.01)));
        assertEquals(NaturalDespawnRules.Decision.DEFER_TO_VANILLA,
                NaturalDespawnRules.evaluate(input(false, false, true, true, true, true, true,
                        Double.POSITIVE_INFINITY, true, true)));
    }

    @Test
    void resetsForWrongOriginOrAnyPersistenceProtection() {
        assertReset(input(false, true, false, false, false, false, false, 80 * 80, false, false));
        assertReset(input(true, false, false, false, false, false, false, 80 * 80, false, false));
        assertReset(input(true, true, true, false, false, false, false, 80 * 80, false, false));
        assertReset(input(true, true, false, true, false, false, false, 80 * 80, false, false));
        assertReset(input(true, true, false, false, true, false, false, 80 * 80, false, false));
        assertReset(input(true, true, false, false, false, true, false, 80 * 80, false, false));
        assertReset(input(true, true, false, false, false, false, true, 80 * 80, false, false));
    }

    @Test
    void targetOrPlayerCombatImmediatelyResetsTheContinuousWindow() {
        assertReset(input(true, true, false, false, false, false, false, 80 * 80, true, false));
        assertReset(input(true, true, false, false, false, false, false, 80 * 80, false, true));
    }

    @Test
    void requiresTheFullThirtySecondWindow() {
        assertFalse(NaturalDespawnRules.readyForAcceleratedRemoval(599));
        assertTrue(NaturalDespawnRules.readyForAcceleratedRemoval(600));
        assertTrue(NaturalDespawnRules.readyForAcceleratedRemoval(601));
        assertThrows(IllegalArgumentException.class,
                () -> NaturalDespawnRules.readyForAcceleratedRemoval(-1));
    }

    @Test
    void rejectsInvalidDistanceEvidence() {
        assertThrows(IllegalArgumentException.class,
                () -> eligible(-0.01));
        assertThrows(IllegalArgumentException.class,
                () -> eligible(Double.NaN));
    }

    private static void assertReset(NaturalDespawnRules.Input input) {
        assertEquals(NaturalDespawnRules.Decision.RESET, NaturalDespawnRules.evaluate(input));
    }

    private static NaturalDespawnRules.Input eligible(double distanceSquared) {
        return input(true, true, false, false, false, false, false, distanceSquared, false, false);
    }

    private static NaturalDespawnRules.Input input(boolean natural, boolean hostile, boolean persistent,
            boolean customPersistent, boolean named, boolean leashed, boolean passenger, double distanceSquared,
            boolean target, boolean recentCombat) {
        return new NaturalDespawnRules.Input(natural, hostile, persistent, customPersistent, named, leashed,
                passenger, distanceSquared, target, recentCombat);
    }
}
