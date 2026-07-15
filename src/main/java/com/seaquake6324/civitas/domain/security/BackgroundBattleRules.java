package com.seaquake6324.civitas.domain.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Pure, reproducible attrition round over an immutable persistent snapshot. */
public final class BackgroundBattleRules {
    public Intent evaluate(int enemies, int round, List<Combatant> combatants, Weights weights, double defeatDraw) {
        if (enemies < 0 || round < 0 || combatants == null) throw new IllegalArgumentException("invalid background battle snapshot");
        double guardPower = combatants.stream().filter(c -> c.role() == InvasionCommitment.Role.GUARD && c.health() > 0)
                .mapToDouble(c -> c.attackScale() * weights.weaponPower() * c.health() / 100.0).sum();
        double exact = weights.enemyDurability() <= 0 ? enemies : guardPower / weights.enemyDurability();
        int defeated = Math.min(enemies, (int)Math.floor(exact) + (defeatDraw < exact - Math.floor(exact) ? 1 : 0));
        int surviving = enemies - defeated;
        if (surviving <= 0) return new Intent(defeated, Map.of(), guardPower, 0);
        ArrayList<Combatant> targets = new ArrayList<>(combatants.stream().filter(c -> c.health() > 0
                && c.role() == InvasionCommitment.Role.GUARD).toList());
        if (targets.isEmpty() || round >= weights.civilianExposureRound()) targets.addAll(combatants.stream()
                .filter(c -> c.health() > 0 && c.role() == InvasionCommitment.Role.CIVILIAN).toList());
        targets.sort(Comparator.comparing(Combatant::citizenId));
        if (targets.isEmpty()) return new Intent(defeated, Map.of(), guardPower, surviving * weights.enemyDamage());
        int targetCount = Math.min(weights.targetsPerRound(), targets.size());
        int start = Math.floorMod(round, targets.size());
        double incoming = surviving * weights.enemyDamage(), share = incoming / targetCount;
        LinkedHashMap<UUID,Integer> damage = new LinkedHashMap<>();
        for (int i = 0; i < targetCount; i++) {
            Combatant target = targets.get((start + i) % targets.size());
            double reduction = Math.min(weights.maximumReduction(), target.armorPieces() * weights.armorReductionPerPiece()
                    + (target.shield() ? weights.shieldReduction() : 0));
            int amount = Math.max(1, (int)Math.round(Math.min(weights.maximumDamagePerRound(), share * (1 - reduction))));
            damage.put(target.citizenId(), amount);
        }
        return new Intent(defeated, Map.copyOf(damage), guardPower, incoming);
    }

    public record Combatant(UUID citizenId, InvasionCommitment.Role role, int health, double attackScale,
            int armorPieces, boolean shield) {
        public Combatant { if (citizenId == null || role == null || health < 0 || health > 100 || attackScale < 0 || armorPieces < 0 || armorPieces > 4) throw new IllegalArgumentException("invalid combatant"); }
    }
    public record Weights(double weaponPower, double enemyDurability, double enemyDamage,
            double armorReductionPerPiece, double shieldReduction, double maximumReduction,
            int targetsPerRound, int maximumDamagePerRound, int civilianExposureRound) {
        public Weights { if (weaponPower < 0 || enemyDurability <= 0 || enemyDamage < 0 || armorReductionPerPiece < 0
                || shieldReduction < 0 || maximumReduction < 0 || maximumReduction > 1 || targetsPerRound < 1
                || maximumDamagePerRound < 1 || civilianExposureRound < 0) throw new IllegalArgumentException("invalid battle weights"); }
    }
    public record Intent(int enemiesDefeated, Map<UUID,Integer> damageByCitizen, double guardPower, double incomingPower) {}
}
