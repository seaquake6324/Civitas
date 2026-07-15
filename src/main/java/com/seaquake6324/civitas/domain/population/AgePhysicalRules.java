package com.seaquake6324.civitas.domain.population;

import java.util.EnumMap;
import java.util.Map;

/**
 * First-pass, explainable vanilla-attribute hypotheses for each citizen age stage.
 * Young adults use the unmodified vanilla player baseline.
 */
public final class AgePhysicalRules {
    public static final double PLAYER_MAX_HEALTH = 20.0;
    public static final double PLAYER_MOVEMENT_SPEED = 0.1;
    public static final double PLAYER_ATTACK_DAMAGE = 1.0;
    public static final double PLAYER_STEP_HEIGHT = 0.6;

    private static final Map<AgeStage, Profile> PROFILES = profiles();

    public Profile forStage(AgeStage stage) {
        Profile profile = PROFILES.get(stage);
        if (profile == null) throw new IllegalArgumentException("Unknown age stage: " + stage);
        return profile;
    }

    private static Map<AgeStage, Profile> profiles() {
        EnumMap<AgeStage, Profile> values = new EnumMap<>(AgeStage.class);
        values.put(AgeStage.CHILD, new Profile(0.62, 10.0, 0.085, 0.5, 0.5));
        values.put(AgeStage.ADOLESCENT, new Profile(0.86, 16.0, 0.105, 0.8, 0.6));
        values.put(AgeStage.YOUNG_ADULT, new Profile(1.00, PLAYER_MAX_HEALTH, PLAYER_MOVEMENT_SPEED,
                PLAYER_ATTACK_DAMAGE, PLAYER_STEP_HEIGHT));
        values.put(AgeStage.MATURE_ADULT, new Profile(1.03, 24.0, 0.095, 1.2, 0.6));
        values.put(AgeStage.ELDER, new Profile(0.95, 14.0, 0.075, 0.7, 0.5));
        return Map.copyOf(values);
    }

    public record Profile(double bodyScale, double maxHealth, double movementSpeed,
                          double attackDamage, double stepHeight) {
        public Profile {
            if (!Double.isFinite(bodyScale) || bodyScale <= 0
                    || !Double.isFinite(maxHealth) || maxHealth <= 0
                    || !Double.isFinite(movementSpeed) || movementSpeed <= 0
                    || !Double.isFinite(attackDamage) || attackDamage < 0
                    || !Double.isFinite(stepHeight) || stepHeight < 0) {
                throw new IllegalArgumentException("Invalid citizen age physical profile");
            }
        }
    }
}
