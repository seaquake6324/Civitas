package com.seaquake6324.civitas.infrastructure.spawn;

import com.seaquake6324.civitas.domain.region.RegionType;
import com.seaquake6324.civitas.domain.spawn.HostileSpawnBudget;
import com.seaquake6324.civitas.infrastructure.civilization.CivilizationAccess;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;

/** Per-spawn-tick hostile accounting with one shared cap and two classified pool ceilings. */
public final class RegionSpawnQuota {
    private static final int VANILLA_SPAWN_AREA = 17 * 17;
    public static final int LOCAL_DIAGNOSTIC_RADIUS = 128;
    private static final long LOCAL_DIAGNOSTIC_RADIUS_SQUARED = (long)LOCAL_DIAGNOSTIC_RADIUS * LOCAL_DIAGNOSTIC_RADIUS;
    private static final Map<NaturalSpawner.SpawnState, TickQuota> BY_STATE = new WeakHashMap<>();
    private static final Map<ServerLevel, LevelMetrics> BY_LEVEL = new WeakHashMap<>();

    public static void beginTick(ServerLevel level, NaturalSpawner.SpawnState state, int spawnableChunks, Iterable<Entity> entities) {
        HostileSpawnBudget.Limits limits = HostileSpawnBudget.limits(MobCategory.MONSTER.getMaxInstancesPerChunk(),
                spawnableChunks, VANILLA_SPAWN_AREA, CivitasConfig.NATURAL_SPAWN_MULTIPLIER.get(),
                HostileSpawnBudget.MAX_POOL_SHARE);
        LevelMetrics metrics = BY_LEVEL.computeIfAbsent(level, ignored -> new LevelMetrics());
        TickQuota quota = new TickQuota(level, limits, spawnableChunks, metrics);
        for (Entity entity : entities) {
            if (entity.getClassification(true) != MobCategory.MONSTER) continue;
            if (entity instanceof Mob mob && (mob.isPersistenceRequired() || mob.requiresCustomPersistence())) continue;
            quota.add(entity.blockPosition());
        }
        metrics.latest = quota.current;
        BY_STATE.put(state, quota);
    }

    public static boolean sharedCapacityAvailable(NaturalSpawner.SpawnState state) {
        TickQuota quota = BY_STATE.get(state);
        return quota == null || quota.globalGateAllowsSpawnCycle();
    }

    public static boolean isTracking(NaturalSpawner.SpawnState state) {
        return BY_STATE.containsKey(state);
    }

    public static boolean canSpawn(NaturalSpawner.SpawnState state, EntityType<?> type, BlockPos pos) {
        if (type.getCategory() != MobCategory.MONSTER) return true;
        TickQuota quota = BY_STATE.get(state);
        return quota == null || quota.canSpawn(pos);
    }

    public static void afterSpawn(NaturalSpawner.SpawnState state, Mob mob) {
        if (mob.getType().getCategory() != MobCategory.MONSTER || !mob.isAddedToLevel()
                || !(mob.level() instanceof ServerLevel level)) return;
        TickQuota quota = BY_STATE.get(state);
        if (quota == null || quota.level != level) return;
        NaturalMobDespawn.markNaturalSpawnerMob(mob);
        quota.add(mob.blockPosition());
        quota.metrics.counters.successful++;
    }

    public static int scaledGlobalCap(int spawnableChunks) {
        return HostileSpawnBudget.limits(MobCategory.MONSTER.getMaxInstancesPerChunk(), spawnableChunks,
                VANILLA_SPAWN_AREA, CivitasConfig.NATURAL_SPAWN_MULTIPLIER.get(),
                HostileSpawnBudget.MAX_POOL_SHARE).totalCap();
    }

    public static Counters counters(ServerLevel level) {
        LevelMetrics metrics = BY_LEVEL.get(level);
        return metrics == null ? Counters.ZERO : metrics.counters.snapshot();
    }

    public static DebugSnapshot debugSnapshot(ServerLevel level, ServerPlayer player, Counters baseline, long windowStart) {
        LevelMetrics metrics = BY_LEVEL.get(level);
        CurrentQuota quota = metrics == null ? null : metrics.latest;
        Counters currentCounters = metrics == null ? Counters.ZERO : metrics.counters.snapshot();
        Counters windowCounters = currentCounters.since(baseline == null ? Counters.ZERO : baseline);
        if (quota == null) {
            return new DebugSnapshot(0, 0, 0, 0, 0, 0, LOCAL_DIAGNOSTIC_RADIUS, 0,
                    CivitasConfig.NATURAL_SPAWN_MULTIPLIER.get().floatValue(),
                    (float)HostileSpawnBudget.MAX_POOL_SHARE,
                    Math.max(0, level.getGameTime() - windowStart), windowCounters);
        }
        int[] local = quota.playerCounts.getOrDefault(player.getUUID(), EMPTY_COUNTS);
        return new DebugSnapshot(quota.counts[0], quota.counts[1], quota.limits.poolCap(), quota.limits.totalCap(),
                local[0], local[1], LOCAL_DIAGNOSTIC_RADIUS, quota.spawnableChunks,
                CivitasConfig.NATURAL_SPAWN_MULTIPLIER.get().floatValue(),
                (float)HostileSpawnBudget.MAX_POOL_SHARE,
                Math.max(0, level.getGameTime() - windowStart), windowCounters);
    }

    public static void recordVanillaDespawn(ServerLevel level) {
        BY_LEVEL.computeIfAbsent(level, ignored -> new LevelMetrics()).counters.vanillaDespawns++;
    }

    public static void recordAcceleratedDespawn(ServerLevel level) {
        BY_LEVEL.computeIfAbsent(level, ignored -> new LevelMetrics()).counters.acceleratedDespawns++;
    }

    private static int pool(ServerLevel level, BlockPos pos) {
        return ServerRegionClassifier.classify(level, pos).type() == RegionType.UNDERGROUND
                ? HostileSpawnBudget.UNDERGROUND_POOL : HostileSpawnBudget.SURFACE_POOL;
    }

    private static long distanceSquared(BlockPos a, BlockPos b) {
        long dx = (long)a.getX() - b.getX();
        long dy = (long)a.getY() - b.getY();
        long dz = (long)a.getZ() - b.getZ();
        return dx * dx + dy * dy + dz * dz;
    }

    private static final int[] EMPTY_COUNTS = new int[2];

    private static final class TickQuota {
        private final ServerLevel level;
        private final HostileSpawnBudget.Limits limits;
        private final int spawnableChunks;
        private final LevelMetrics metrics;
        private final int[] counts = new int[2];
        private final Map<UUID, int[]> playerCounts = new java.util.HashMap<>();
        private final CurrentQuota current;

        private TickQuota(ServerLevel level, HostileSpawnBudget.Limits limits, int spawnableChunks, LevelMetrics metrics) {
            this.level = level;
            this.limits = limits;
            this.spawnableChunks = spawnableChunks;
            this.metrics = metrics;
            this.current = new CurrentQuota(limits, spawnableChunks, counts, playerCounts);
        }

        private void add(BlockPos pos) {
            int pool = pool(level, pos);
            counts[pool]++;
            for (ServerPlayer player : level.players()) {
                if (distanceSquared(player.blockPosition(), pos) <= LOCAL_DIAGNOSTIC_RADIUS_SQUARED) {
                    playerCounts.computeIfAbsent(player.getUUID(), ignored -> new int[2])[pool]++;
                }
            }
        }

        private boolean globalGateAllowsSpawnCycle() {
            long total = (long)counts[0] + counts[1];
            if (total >= limits.totalCap()) {
                metrics.counters.totalCapRejected++;
                return false;
            }
            if (counts[0] >= limits.poolCap() && counts[1] >= limits.poolCap()) {
                metrics.counters.poolCapRejected++;
                return false;
            }
            return true;
        }

        private boolean canSpawn(BlockPos pos) {
            metrics.counters.attempts++;
            int pool = pool(level, pos);
            HostileSpawnBudget.Decision capacity = HostileSpawnBudget.evaluate(limits, counts[0], counts[1],
                    pool, 0.0, 0.5);
            if (capacity == HostileSpawnBudget.Decision.TOTAL_CAP) {
                metrics.counters.totalCapRejected++;
                return false;
            }
            if (capacity == HostileSpawnBudget.Decision.POOL_CAP) {
                metrics.counters.poolCapRejected++;
                return false;
            }
            double suppression = CivilizationAccess.suppression(level, pos);
            HostileSpawnBudget.Decision decision = HostileSpawnBudget.evaluate(limits, counts[0], counts[1],
                    pool, suppression, level.getRandom().nextDouble());
            if (decision == HostileSpawnBudget.Decision.CIVILITY) {
                metrics.counters.civilityRejected++;
                return false;
            }
            return decision == HostileSpawnBudget.Decision.ALLOW;
        }
    }

    private static final class LevelMetrics {
        private CurrentQuota latest;
        private final MutableCounters counters = new MutableCounters();
    }

    /** Detached from ServerLevel so the WeakHashMap key remains collectible when a world unloads. */
    private static final class CurrentQuota {
        private final HostileSpawnBudget.Limits limits;
        private final int spawnableChunks;
        private final int[] counts;
        private final Map<UUID, int[]> playerCounts;

        private CurrentQuota(HostileSpawnBudget.Limits limits, int spawnableChunks, int[] counts,
                Map<UUID, int[]> playerCounts) {
            this.limits = limits;
            this.spawnableChunks = spawnableChunks;
            this.counts = counts;
            this.playerCounts = playerCounts;
        }
    }

    private static final class MutableCounters {
        private long attempts;
        private long totalCapRejected;
        private long poolCapRejected;
        private long civilityRejected;
        private long successful;
        private long vanillaDespawns;
        private long acceleratedDespawns;

        private Counters snapshot() {
            return new Counters(attempts, totalCapRejected, poolCapRejected, civilityRejected,
                    successful, vanillaDespawns, acceleratedDespawns);
        }
    }

    public record Counters(long attempts, long totalCapRejected, long poolCapRejected,
            long civilityRejected, long successful, long vanillaDespawns, long acceleratedDespawns) {
        public static final Counters ZERO = new Counters(0, 0, 0, 0, 0, 0, 0);

        public Counters since(Counters baseline) {
            return new Counters(delta(attempts, baseline.attempts), delta(totalCapRejected, baseline.totalCapRejected),
                    delta(poolCapRejected, baseline.poolCapRejected), delta(civilityRejected, baseline.civilityRejected),
                    delta(successful, baseline.successful), delta(vanillaDespawns, baseline.vanillaDespawns),
                    delta(acceleratedDespawns, baseline.acceleratedDespawns));
        }

        private static long delta(long current, long baseline) { return Math.max(0, current - baseline); }
    }

    public record DebugSnapshot(int surfaceCount, int undergroundCount, int poolLimit, int totalLimit,
            int localSurfaceCount, int localUndergroundCount, int localRadius, int spawnableChunks,
            float multiplier, float poolShare, long windowTicks, Counters counters) {
        public int totalCount() { return surfaceCount + undergroundCount; }
    }

    private RegionSpawnQuota() {}
}
