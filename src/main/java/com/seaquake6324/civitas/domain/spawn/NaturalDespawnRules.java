package com.seaquake6324.civitas.domain.spawn;

/** Pure eligibility rules for Civitas' accelerated removal of ineffective natural hostile mobs. */
public final class NaturalDespawnRules {
    public static final int REQUIRED_CONSECUTIVE_TICKS = 30 * 20;
    public static final int MINIMUM_DISTANCE = 32;
    public static final double MINIMUM_DISTANCE_SQUARED = (double)MINIMUM_DISTANCE * MINIMUM_DISTANCE;
    public static final double VANILLA_INSTANT_DISTANCE_SQUARED = 128.0 * 128.0;

    public enum Decision {
        RESET,
        COUNT,
        DEFER_TO_VANILLA
    }

    public record Input(
            boolean naturalSpawnerMarked,
            boolean hostile,
            boolean persistenceRequired,
            boolean customPersistenceRequired,
            boolean named,
            boolean leashed,
            boolean passenger,
            double nearestPlayerDistanceSquared,
            boolean hasAttackTarget,
            boolean recentPlayerCombat
    ) {
        public Input {
            if (Double.isNaN(nearestPlayerDistanceSquared) || nearestPlayerDistanceSquared < 0.0) {
                throw new IllegalArgumentException("Nearest-player distance must be non-negative");
            }
        }
    }

    public static Decision evaluate(Input input) {
        if (input == null) throw new IllegalArgumentException("Despawn input is required");
        if (input.nearestPlayerDistanceSquared() > VANILLA_INSTANT_DISTANCE_SQUARED) {
            return Decision.DEFER_TO_VANILLA;
        }
        if (!input.naturalSpawnerMarked()
                || !input.hostile()
                || input.persistenceRequired()
                || input.customPersistenceRequired()
                || input.named()
                || input.leashed()
                || input.passenger()
                || input.nearestPlayerDistanceSquared() < MINIMUM_DISTANCE_SQUARED
                || input.hasAttackTarget()
                || input.recentPlayerCombat()) {
            return Decision.RESET;
        }
        return Decision.COUNT;
    }

    public static boolean readyForAcceleratedRemoval(int consecutiveEligibleTicks) {
        if (consecutiveEligibleTicks < 0) throw new IllegalArgumentException("Eligible ticks cannot be negative");
        return consecutiveEligibleTicks >= REQUIRED_CONSECUTIVE_TICKS;
    }

    private NaturalDespawnRules() {}
}
