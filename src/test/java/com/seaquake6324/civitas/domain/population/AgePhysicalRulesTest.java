package com.seaquake6324.civitas.domain.population;

import static org.junit.jupiter.api.Assertions.*;

import java.util.EnumSet;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

class AgePhysicalRulesTest {
    private final AgePhysicalRules rules = new AgePhysicalRules();

    @Test void youngAdultMatchesVanillaPlayerBaseline() {
        var young = rules.forStage(AgeStage.YOUNG_ADULT);
        assertEquals(AgePhysicalRules.PLAYER_MAX_HEALTH, young.maxHealth());
        assertEquals(AgePhysicalRules.PLAYER_MOVEMENT_SPEED, young.movementSpeed());
        assertEquals(AgePhysicalRules.PLAYER_ATTACK_DAMAGE, young.attackDamage());
        assertEquals(AgePhysicalRules.PLAYER_STEP_HEIGHT, young.stepHeight());
        assertEquals(1.0, young.bodyScale());
    }

    @Test void everyAgeStageHasAValidAndVisiblyDistinctBodyScale() {
        var scales = new HashSet<Double>();
        for (AgeStage stage : EnumSet.allOf(AgeStage.class)) {
            var profile = rules.forStage(stage);
            assertTrue(profile.bodyScale() > 0);
            assertTrue(profile.maxHealth() > 0);
            assertTrue(profile.movementSpeed() > 0);
            assertTrue(profile.attackDamage() >= 0);
            assertTrue(profile.stepHeight() >= 0);
            assertTrue(scales.add(profile.bodyScale()), () -> "duplicate scale for " + stage);
        }
    }

    @Test void lifeAndMovementFollowTheFirstPassAgeHypothesis() {
        var child = rules.forStage(AgeStage.CHILD);
        var adolescent = rules.forStage(AgeStage.ADOLESCENT);
        var young = rules.forStage(AgeStage.YOUNG_ADULT);
        var mature = rules.forStage(AgeStage.MATURE_ADULT);
        var elder = rules.forStage(AgeStage.ELDER);

        assertTrue(child.maxHealth() < adolescent.maxHealth());
        assertTrue(adolescent.maxHealth() < young.maxHealth());
        assertTrue(mature.maxHealth() > young.maxHealth());
        assertTrue(elder.maxHealth() < young.maxHealth());
        assertTrue(adolescent.movementSpeed() > young.movementSpeed());
        assertTrue(mature.movementSpeed() < young.movementSpeed());
        assertTrue(elder.movementSpeed() < mature.movementSpeed());
    }
}
