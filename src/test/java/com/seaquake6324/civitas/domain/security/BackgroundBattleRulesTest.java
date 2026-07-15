package com.seaquake6324.civitas.domain.security;

import static org.junit.jupiter.api.Assertions.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BackgroundBattleRulesTest {
    private final BackgroundBattleRules rules = new BackgroundBattleRules();
    private final BackgroundBattleRules.Weights weights = new BackgroundBattleRules.Weights(2, 4, 6, .08, .15, .65, 2, 35, 3);

    @Test void armedGuardDefeatsEnemiesAndAbsorbsEarlyDamage() {
        UUID guard = UUID.randomUUID(), civilian = UUID.randomUUID();
        var intent = rules.evaluate(4, 0, List.of(
                new BackgroundBattleRules.Combatant(guard, InvasionCommitment.Role.GUARD, 100, 3, 2, true),
                new BackgroundBattleRules.Combatant(civilian, InvasionCommitment.Role.CIVILIAN, 100, 0, 0, false)), weights, 0);
        assertTrue(intent.enemiesDefeated() > 0);
        assertTrue(intent.damageByCitizen().containsKey(guard));
        assertFalse(intent.damageByCitizen().containsKey(civilian));
    }

    @Test void civiliansBecomeExposedAfterConfiguredRounds() {
        UUID civilian = UUID.randomUUID();
        var intent = rules.evaluate(3, 3, List.of(new BackgroundBattleRules.Combatant(civilian,
                InvasionCommitment.Role.CIVILIAN, 100, 0, 0, false)), weights, .9);
        assertTrue(intent.damageByCitizen().containsKey(civilian));
    }

    @Test void armorAndShieldReduceDamage() {
        UUID armored = new UUID(0,1), bare = new UUID(0,2);
        var intent = rules.evaluate(10, 4, List.of(
                new BackgroundBattleRules.Combatant(armored, InvasionCommitment.Role.CIVILIAN, 100, 0, 4, true),
                new BackgroundBattleRules.Combatant(bare, InvasionCommitment.Role.CIVILIAN, 100, 0, 0, false)), weights, .9);
        assertTrue(intent.damageByCitizen().get(armored) < intent.damageByCitizen().get(bare));
    }
}
