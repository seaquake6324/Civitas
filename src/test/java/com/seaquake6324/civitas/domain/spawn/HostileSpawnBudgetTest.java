package com.seaquake6324.civitas.domain.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class HostileSpawnBudgetTest {
    @Test
    void floorsTheSharedVanillaScaledCapAndSixtyPercentPoolLimit() {
        assertEquals(new HostileSpawnBudget.Limits(70, 42),
                HostileSpawnBudget.limits(70, 289, 289, 1.0, 0.60));
        assertEquals(new HostileSpawnBudget.Limits(24, 14),
                HostileSpawnBudget.limits(70, 100, 289, 1.0, 0.60));
    }

    @Test
    void sharesOneTotalWhileAllowingTheOtherPoolToUseReservedCapacity() {
        HostileSpawnBudget.Limits limits = new HostileSpawnBudget.Limits(70, 42);

        assertEquals(HostileSpawnBudget.Decision.ALLOW,
                HostileSpawnBudget.evaluate(limits, 41, 28, HostileSpawnBudget.SURFACE_POOL, 0.0, 0.5));
        assertEquals(HostileSpawnBudget.Decision.POOL_CAP,
                HostileSpawnBudget.evaluate(limits, 42, 0, HostileSpawnBudget.SURFACE_POOL, 0.0, 0.5));
        assertEquals(HostileSpawnBudget.Decision.ALLOW,
                HostileSpawnBudget.evaluate(limits, 42, 0, HostileSpawnBudget.UNDERGROUND_POOL, 0.0, 0.5));
    }

    @Test
    void reportsOneDeterministicRejectionReasonInPrecedenceOrder() {
        HostileSpawnBudget.Limits limits = new HostileSpawnBudget.Limits(70, 42);

        assertEquals(HostileSpawnBudget.Decision.TOTAL_CAP,
                HostileSpawnBudget.evaluate(limits, 42, 28, HostileSpawnBudget.SURFACE_POOL, 1.0, 0.0));
        assertEquals(HostileSpawnBudget.Decision.POOL_CAP,
                HostileSpawnBudget.evaluate(limits, 42, 1, HostileSpawnBudget.SURFACE_POOL, 1.0, 0.0));
        assertEquals(HostileSpawnBudget.Decision.CIVILITY,
                HostileSpawnBudget.evaluate(limits, 1, 1, HostileSpawnBudget.SURFACE_POOL, 0.50, 0.49));
        assertEquals(HostileSpawnBudget.Decision.ALLOW,
                HostileSpawnBudget.evaluate(limits, 1, 1, HostileSpawnBudget.SURFACE_POOL, 0.50, 0.50));
    }

    @Test
    void rejectsInvalidConfigurationAndInputs() {
        assertThrows(IllegalArgumentException.class,
                () -> HostileSpawnBudget.limits(70, 289, 0, 1.0, 0.60));
        assertThrows(IllegalArgumentException.class,
                () -> HostileSpawnBudget.limits(70, 289, 289, -1.0, 0.60));
        assertThrows(IllegalArgumentException.class,
                () -> HostileSpawnBudget.limits(70, 289, 289, 1.0, 1.01));

        HostileSpawnBudget.Limits limits = new HostileSpawnBudget.Limits(70, 42);
        assertThrows(IllegalArgumentException.class,
                () -> HostileSpawnBudget.evaluate(limits, -1, 0, 0, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> HostileSpawnBudget.evaluate(limits, 0, 0, 2, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class,
                () -> HostileSpawnBudget.evaluate(limits, 0, 0, 0, 0.0, 1.0));
    }
}
