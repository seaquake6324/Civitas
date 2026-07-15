package com.seaquake6324.civitas.infrastructure.spawn;

import com.seaquake6324.civitas.domain.spawn.NaturalDespawnRules;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.entity.living.MobDespawnEvent;
import com.seaquake6324.civitas.infrastructure.border.InvasionMobMarker;

/** Tracks only hostiles proven to have been added by NaturalSpawner's regular callback. */
public final class NaturalMobDespawn {
    private static final String MARKER_KEY = "civitas:natural_spawner";
    private static final String SCHEMA_VERSION_KEY = "SchemaVersion";
    private static final String MARKED_KEY = "Marked";
    private static final int MARKER_SCHEMA_VERSION = 1;
    private static final Map<Mob, Integer> ELIGIBLE_TICKS = new WeakHashMap<>();
    private static final Set<Mob> ACCELERATED_REMOVALS = Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<Mob> EXTERNAL_REMOVALS = Collections.newSetFromMap(new WeakHashMap<>());

    public static void markNaturalSpawnerMob(Mob mob) {
        if (mob.getType().getCategory() != MobCategory.MONSTER || !mob.isAddedToLevel()) return;
        CompoundTag marker = new CompoundTag();
        marker.putInt(SCHEMA_VERSION_KEY, MARKER_SCHEMA_VERSION);
        marker.putBoolean(MARKED_KEY, true);
        mob.getPersistentData().put(MARKER_KEY, marker);
    }

    public static boolean isMarked(Mob mob) {
        return mob.getPersistentData().getCompound(MARKER_KEY)
                .filter(marker -> marker.getIntOr(SCHEMA_VERSION_KEY, 0) == MARKER_SCHEMA_VERSION)
                .map(marker -> marker.getBooleanOr(MARKED_KEY, false))
                .orElse(false);
    }

    /** Called at LOWEST priority so another mod's explicit despawn decision remains authoritative. */
    public static void onDespawnCheck(MobDespawnEvent event) {
        Mob mob = event.getEntity();
        if (InvasionMobMarker.active(mob)) { event.setResult(MobDespawnEvent.Result.DENY); return; }
        ACCELERATED_REMOVALS.remove(mob);
        EXTERNAL_REMOVALS.remove(mob);
        if (!isMarked(mob) || mob.getType().getCategory() != MobCategory.MONSTER) {
            reset(mob);
            return;
        }
        if (event.getResult() != MobDespawnEvent.Result.DEFAULT) {
            ELIGIBLE_TICKS.remove(mob);
            if (event.getResult() == MobDespawnEvent.Result.ALLOW) EXTERNAL_REMOVALS.add(mob);
            return;
        }
        if (!(mob.level() instanceof ServerLevel level)) {
            reset(mob);
            return;
        }

        double nearestPlayerDistanceSquared = nearestPlayerDistanceSquared(level, mob);
        boolean recentPlayerCombat = mob.hurtTime > 0 && mob.getLastHurtByPlayer() != null
                || mob.getLastHurtMob() instanceof Player && mob.tickCount - mob.getLastHurtMobTimestamp() <= 1
                || mob.getLastHurtByMob() instanceof Player && mob.tickCount - mob.getLastHurtByMobTimestamp() <= 1;
        boolean hasTarget = mob.getTarget() != null || mob.getBrain().hasMemoryValue(MemoryModuleType.ATTACK_TARGET);
        NaturalDespawnRules.Decision decision = NaturalDespawnRules.evaluate(new NaturalDespawnRules.Input(
                true, true, mob.isPersistenceRequired(), mob.requiresCustomPersistence(), mob.hasCustomName(),
                mob.isLeashed(), mob.isPassenger(), nearestPlayerDistanceSquared, hasTarget, recentPlayerCombat));
        if (decision != NaturalDespawnRules.Decision.COUNT || !mob.removeWhenFarAway(nearestPlayerDistanceSquared)) {
            reset(mob);
            return;
        }

        int ticks = ELIGIBLE_TICKS.merge(mob, 1, Integer::sum);
        if (NaturalDespawnRules.readyForAcceleratedRemoval(ticks)) {
            ELIGIBLE_TICKS.remove(mob);
            ACCELERATED_REMOVALS.add(mob);
            event.setResult(MobDespawnEvent.Result.ALLOW);
        }
    }

    public static void recordCombat(Entity entity) {
        if (entity instanceof Mob mob && isMarked(mob)) reset(mob);
    }

    public static boolean consumeAcceleratedRemoval(Mob mob) {
        ELIGIBLE_TICKS.remove(mob);
        return ACCELERATED_REMOVALS.remove(mob);
    }

    public static boolean consumeExternalRemoval(Mob mob) {
        ELIGIBLE_TICKS.remove(mob);
        return EXTERNAL_REMOVALS.remove(mob);
    }

    private static void reset(Mob mob) {
        ELIGIBLE_TICKS.remove(mob);
        ACCELERATED_REMOVALS.remove(mob);
        EXTERNAL_REMOVALS.remove(mob);
    }

    private static double nearestPlayerDistanceSquared(ServerLevel level, Mob mob) {
        double nearest = Double.POSITIVE_INFINITY;
        for (ServerPlayer player : level.players()) nearest = Math.min(nearest, player.distanceToSqr(mob));
        return nearest;
    }

    private NaturalMobDespawn() {}
}
