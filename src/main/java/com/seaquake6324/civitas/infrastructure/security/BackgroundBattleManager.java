package com.seaquake6324.civitas.infrastructure.security;

import com.seaquake6324.civitas.application.UpdateCitizenHealthService;
import com.seaquake6324.civitas.domain.population.AgePhysicalRules;
import com.seaquake6324.civitas.domain.population.CitizenRecord;
import com.seaquake6324.civitas.domain.security.BackgroundBattleRules;
import com.seaquake6324.civitas.domain.security.InvasionCommitment;
import com.seaquake6324.civitas.infrastructure.border.InvasionMobIndex;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.CitizenEquipmentSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.ThreatSavedData;
import com.seaquake6324.civitas.infrastructure.population.CitizenCombatDeathManager;
import com.seaquake6324.civitas.infrastructure.population.PopulationAgingManager;
import java.util.ArrayList;
import java.util.SplittableRandom;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/** Bounded main-thread adapter for committed invasions whose source chunk is not loaded. */
public final class BackgroundBattleManager {
    private static final BackgroundBattleRules RULES = new BackgroundBattleRules();
    private static final UpdateCitizenHealthService HEALTH = new UpdateCitizenHealthService();
    private static UUID cursor;
    private static long batches, examined, rounds, loadedSkips, incompleteSkips, intervalSkips, stale,
            enemyDefeats, citizenDamage, citizenDeaths, totalMicros, maximumMicros;
    private static String lastReason = "not_run";

    public static void tick(MinecraftServer server) {
        long began = System.nanoTime();
        ThreatSavedData threat = ThreatSavedData.get(server);
        PopulationSavedData population = PopulationSavedData.get(server);
        CitizenEquipmentSavedData equipment = CitizenEquipmentSavedData.get(server);
        if (threat.readOnly() || population.readOnly() || equipment.readOnly()) {
            lastReason = "read_only";
            finish(began);
            return;
        }
        var batch = threat.invasionCommitmentBatch(cursor, CivitasConfig.BACKGROUND_BATTLES_PER_TICK.get());
        cursor = batch.nextCursor(); batches++; examined += batch.examined();
        long now = server.overworld().getGameTime();
        for (InvasionCommitment commitment : batch.commitments())
            process(server, threat, population, equipment, commitment, now);
        finish(began);
    }

    private static void process(MinecraftServer server, ThreatSavedData threat, PopulationSavedData population,
            CitizenEquipmentSavedData equipment, InvasionCommitment commitment, long now) {
        if (now - commitment.lastRoundAt() < CivitasConfig.BACKGROUND_BATTLE_ROUND_TICKS.get()) {
            intervalSkips++; lastReason = "round_interval"; return;
        }
        var city = CitySavedData.get(server).byId(commitment.cityId()).orElse(null);
        ServerLevel level = city == null ? null : level(server, city.dimension());
        if (level == null) { stale++; lastReason = "missing_city_or_dimension"; return; }
        BlockPos site = BlockPos.of(commitment.location());
        var loaded = InvasionMobIndex.count(level, commitment.invasionId());
        if (loaded.incomplete()) { incompleteSkips++; lastReason = "mob_index_incomplete"; return; }
        if (loaded.loadedAlive() > 0 || level.hasChunkAt(site)) {
            loadedSkips++; lastReason = "visible_world_authoritative"; return;
        }
        ArrayList<BackgroundBattleRules.Combatant> combatants = new ArrayList<>();
        ArrayList<CitizenRecord> snapshot = new ArrayList<>();
        for (InvasionCommitment.Participant participant : commitment.participants()) {
            CitizenRecord citizen = population.citizen(participant.citizenId()).orElse(null);
            if (citizen == null || !citizen.alive() || !commitment.cityId().equals(citizen.cityId())) continue;
            var gear = equipment.guardEquipment(citizen.id(), server.registryAccess());
            var stage = PopulationAgingManager.rules().evaluate(citizen.ageTicks()).stage();
            double attack = new AgePhysicalRules().forStage(stage).attackDamage() / AgePhysicalRules.PLAYER_ATTACK_DAMAGE;
            if (participant.role() != InvasionCommitment.Role.GUARD || !gear.basicWeapon()) attack = 0;
            combatants.add(new BackgroundBattleRules.Combatant(citizen.id(), participant.role(), citizen.health(),
                    attack, gear.armorPieces(), gear.shield()));
            snapshot.add(citizen);
        }
        var intent = RULES.evaluate(commitment.survivingEnemyIds().size(), commitment.round(), combatants,
                weights(), draw(commitment));
        if (!threat.applyBackgroundRound(commitment.invasionId(), commitment.revision(), intent.enemiesDefeated(), now)) {
            stale++; lastReason = "stale_commitment"; return;
        }
        for (CitizenRecord citizen : snapshot) {
            int damage = intent.damageByCitizen().getOrDefault(citizen.id(), 0);
            if (damage <= 0) continue;
            citizenDamage += damage;
            if (citizen.health() <= damage) {
                if (CitizenCombatDeathManager.killVirtual(server, citizen.id(), commitment.invasionId(), now)) citizenDeaths++;
                else stale++;
            } else if (!HEALTH.update(population, citizen.id(), citizen.revision(), citizen.health() - damage).success()) stale++;
        }
        rounds++; enemyDefeats += intent.enemiesDefeated(); lastReason = "round_applied";
    }

    private static BackgroundBattleRules.Weights weights() {
        return new BackgroundBattleRules.Weights(CivitasConfig.BACKGROUND_BATTLE_WEAPON_POWER.get(),
                CivitasConfig.BACKGROUND_BATTLE_ENEMY_DURABILITY.get(), CivitasConfig.BACKGROUND_BATTLE_ENEMY_DAMAGE.get(),
                CivitasConfig.BACKGROUND_BATTLE_ARMOR_REDUCTION_PER_PIECE.get(),
                CivitasConfig.BACKGROUND_BATTLE_SHIELD_REDUCTION.get(), CivitasConfig.BACKGROUND_BATTLE_MAXIMUM_REDUCTION.get(),
                CivitasConfig.BACKGROUND_BATTLE_TARGETS_PER_ROUND.get(),
                CivitasConfig.BACKGROUND_BATTLE_MAX_DAMAGE_PER_ROUND.get(),
                CivitasConfig.BACKGROUND_BATTLE_CIVILIAN_EXPOSURE_ROUND.get());
    }

    private static double draw(InvasionCommitment commitment) {
        long seed = commitment.invasionId().getMostSignificantBits()
                ^ Long.rotateLeft(commitment.invasionId().getLeastSignificantBits(), 17)
                ^ ((long) commitment.wave() << 32) ^ commitment.round();
        return new SplittableRandom(seed).nextDouble();
    }
    private static ServerLevel level(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels())
            if (level.dimension().identifier().toString().equals(dimension)) return level;
        return null;
    }
    private static void finish(long began) { long micros = Math.max(0, (System.nanoTime() - began) / 1_000);
        totalMicros += micros; maximumMicros = Math.max(maximumMicros, micros); }
    public static Metrics metrics() { return new Metrics(batches, examined, rounds, loadedSkips, incompleteSkips,
            intervalSkips, stale, enemyDefeats, citizenDamage, citizenDeaths, totalMicros, maximumMicros, lastReason); }
    public record Metrics(long batches,long examined,long rounds,long loadedSkips,long incompleteSkips,long intervalSkips,
            long stale,long enemyDefeats,long citizenDamage,long citizenDeaths,long totalMicros,long maximumMicros,String lastReason) {}
    private BackgroundBattleManager() {}
}
