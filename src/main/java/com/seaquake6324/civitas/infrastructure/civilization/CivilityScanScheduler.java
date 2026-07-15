package com.seaquake6324.civitas.infrastructure.civilization;

import com.seaquake6324.civitas.application.CivilityEvaluationService;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.civilization.BoundaryPorts;
import com.seaquake6324.civitas.domain.civilization.ChunkCivilization;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CivilizationSavedData;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** Server scheduler: one layer scan per tick, loaded-only, with overflow recovery. */
public final class CivilityScanScheduler {
    private static final CivilityDirtyQueue QUEUE = new CivilityDirtyQueue();
    private static final BoundedCivilityScanner SCANNER = new BoundedCivilityScanner();
    private static final CivilityEvaluationService EVALUATION = new CivilityEvaluationService();
    private static final Set<String> PROGRESSIVE_RESCAN = new HashSet<>();
    private static final Map<String, Metrics> METRICS = new HashMap<>();
    private static long lastReconcile = Long.MIN_VALUE;

    public static void markDirty(ServerLevel level, int chunkX, int chunkZ, CivilityDirtyQueue.Reason reason) {
        long now = level.getGameTime();
        String dimension = level.dimension().identifier().toString();
        long chunk = ChunkCoordinate.pack(chunkX, chunkZ);
        if (CitySavedData.get(level.getServer()).cityAt(dimension, chunk).isEmpty()) return;
        for (CivilizationLayer layer : CivilizationLayer.values()) {
            if (!QUEUE.offer(new CivilityDirtyQueue.Key(dimension, chunk, layer), now, reason)) PROGRESSIVE_RESCAN.add(dimension);
        }
    }

    public static void markNear(ServerLevel level, net.minecraft.core.BlockPos pos) {
        int cx = pos.getX() >> 4, cz = pos.getZ() >> 4;
        markDirty(level, cx, cz, CivilityDirtyQueue.Reason.BLOCK_CHANGE);
        markDirty(level, cx + 1, cz, CivilityDirtyQueue.Reason.BLOCK_CHANGE);
        markDirty(level, cx - 1, cz, CivilityDirtyQueue.Reason.BLOCK_CHANGE);
        markDirty(level, cx, cz + 1, CivilityDirtyQueue.Reason.BLOCK_CHANGE);
        markDirty(level, cx, cz - 1, CivilityDirtyQueue.Reason.BLOCK_CHANGE);
    }

    public static void tick(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        long now = overworld.getGameTime();
        if (lastReconcile == Long.MIN_VALUE || now - lastReconcile >= 200) {
            reconcile(server, now);
            lastReconcile = now;
        }
        long budgetStarted = System.nanoTime();
        int attempts = Math.min(16, QUEUE.size());
        long softBudgetNanos = CivitasConfig.CIVILITY_SCAN_BUDGET_MICROS.get() * 1_000L;
        while (attempts-- > 0 && System.nanoTime() - budgetStarted < softBudgetNanos) {
            Optional<CivilityDirtyQueue.Task> next = QUEUE.poll(key -> playerDistance(server, key));
            if (next.isEmpty() || execute(server, next.get())) return;
        }
    }

    private static boolean execute(MinecraftServer server, CivilityDirtyQueue.Task task) {
        CivilityDirtyQueue.Key key = task.key();
        ServerLevel level = level(server, key.dimension());
        ChunkPos pos = chunkPos(key.chunk());
        Metrics metrics = METRICS.computeIfAbsent(key.dimension(), ignored -> new Metrics());
        if (level == null || !level.hasChunk(pos.x(), pos.z())) {
            metrics.unloadedDeferrals++;
            QUEUE.defer(task);
            return false;
        }
        Optional<City> owner = CitySavedData.get(server).cityAt(key.dimension(), key.chunk());
        if (owner.isEmpty()) {
            CivilizationSavedData.get(server).removeChunk(key.dimension(), key.chunk());
            return false;
        }
        CivilizationSavedData data = CivilizationSavedData.get(server);
        long started = System.nanoTime();
        BoundedCivilityScanner.ScanResult result = SCANNER.scan(level, owner.get(), key.chunk(), key.layer(),
                neighbors(data, owner.get(), key));
        if (result == null) return false;
        ChunkCivilization previous = data.get(key.dimension(), key.chunk(), key.layer());
        ChunkCivilization evaluated = EVALUATION.evaluate(previous,
                new CivilityEvaluationService.ScanSnapshot(result.fingerprint(), result.evidence()),
                level.getGameTime(), CivitasConfig.CIVILITY_STABILITY_TICKS.get(),
                CivitasConfig.CIVILITY_GROWTH_PER_CYCLE.get(), CivitasConfig.CIVILITY_DECLINE_PER_CYCLE.get(),
                new com.seaquake6324.civitas.domain.civilization.CivilityScoringRules.Weights(CivitasConfig.CIVILITY_BUILDING_WEIGHT.get(),CivitasConfig.CIVILITY_FACILITIES_WEIGHT.get(),CivitasConfig.CIVILITY_SAFETY_WEIGHT.get(),CivitasConfig.CIVILITY_CONNECTIVITY_WEIGHT.get()));
        data.put(key.dimension(), key.chunk(), key.layer(), evaluated);
        long elapsed = System.nanoTime() - started;
        metrics.record(elapsed, result.evidence().visitedCells(), task);
        if (!previous.boundaryPorts().equals(evaluated.boundaryPorts())) markNeighbors(level, pos, CivilityDirtyQueue.Reason.BORDER_CHANGE);
        return true;
    }

    private static BoundedCivilityScanner.NeighborPorts neighbors(CivilizationSavedData data, City city,
            CivilityDirtyQueue.Key key) {
        ChunkPos pos = chunkPos(key.chunk());
        return new BoundedCivilityScanner.NeighborPorts() {
            public boolean claimed(BoundaryPorts.Direction direction) { return city.territory().contains(adjacent(pos, direction)); }
            public BoundaryPorts ports(BoundaryPorts.Direction direction) {
                return data.get(key.dimension(), adjacent(pos, direction), key.layer()).boundaryPorts();
            }
        };
    }

    private static void reconcile(MinecraftServer server, long now) {
        CivilizationSavedData civilization = CivilizationSavedData.get(server);
        for (City city : CitySavedData.get(server).cities()) {
            ServerLevel level = level(server, city.dimension());
            if (level == null) continue;
            boolean recovery = PROGRESSIVE_RESCAN.contains(city.dimension());
            for (long chunk : city.territory()) {
                ChunkPos pos = chunkPos(chunk);
                if (!level.hasChunk(pos.x(), pos.z())) continue;
                for (CivilizationLayer layer : CivilizationLayer.values()) {
                    ChunkCivilization state = civilization.get(city.dimension(), chunk, layer);
                    ChunkCivilization evolved = EVALUATION.evolve(state, now,
                            CivitasConfig.CIVILITY_GROWTH_PER_CYCLE.get(), CivitasConfig.CIVILITY_DECLINE_PER_CYCLE.get());
                    if (!evolved.equals(state)) civilization.put(city.dimension(), chunk, layer, evolved);
                    if (recovery || state.lastScanned() == 0 || state.candidate() != null)
                        QUEUE.offer(new CivilityDirtyQueue.Key(city.dimension(), chunk, layer), now,
                                recovery ? CivilityDirtyQueue.Reason.MIGRATION_RESCAN : CivilityDirtyQueue.Reason.FOUNDING);
                }
            }
            if (recovery && QUEUE.size() < CivilityDirtyQueue.DEFAULT_CAPACITY / 2) PROGRESSIVE_RESCAN.remove(city.dimension());
        }
    }

    private static void markNeighbors(ServerLevel level, ChunkPos pos, CivilityDirtyQueue.Reason reason) {
        markDirty(level, pos.x() + 1, pos.z(), reason); markDirty(level, pos.x() - 1, pos.z(), reason);
        markDirty(level, pos.x(), pos.z() + 1, reason); markDirty(level, pos.x(), pos.z() - 1, reason);
    }
    private static long adjacent(ChunkPos pos, BoundaryPorts.Direction direction) {
        return switch (direction) {
            case NORTH -> ChunkCoordinate.pack(pos.x(), pos.z() - 1); case EAST -> ChunkCoordinate.pack(pos.x() + 1, pos.z());
            case SOUTH -> ChunkCoordinate.pack(pos.x(), pos.z() + 1); case WEST -> ChunkCoordinate.pack(pos.x() - 1, pos.z());
        };
    }
    private static ServerLevel level(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels()) if (level.dimension().identifier().toString().equals(dimension)) return level;
        return null;
    }
    private static long playerDistance(MinecraftServer server, CivilityDirtyQueue.Key key) {
        ServerLevel level = level(server, key.dimension());
        if (level == null) return Long.MAX_VALUE;
        ChunkPos pos = chunkPos(key.chunk());
        return level.players().stream().mapToLong(player -> {
            long dx = (player.blockPosition().getX() >> 4) - pos.x();
            long dz = (player.blockPosition().getZ() >> 4) - pos.z();
            return dx * dx + dz * dz;
        }).min().orElse(Long.MAX_VALUE - 1);
    }
    private static ChunkPos chunkPos(long packed) {
        ChunkCoordinate coordinate = ChunkCoordinate.unpack(packed);
        return new ChunkPos(coordinate.x(), coordinate.z());
    }

    public static Snapshot snapshot(String dimension, long now) {
        Metrics m = METRICS.getOrDefault(dimension, new Metrics());
        return new Snapshot(QUEUE.size(), QUEUE.oldestAge(now), m.lastNanos, m.count == 0 ? 0 : m.totalNanos / m.count,
                m.maxNanos, m.lastVisited, m.unloadedDeferrals, PROGRESSIVE_RESCAN.contains(dimension),
                m.lastReasons, m.lastDeferrals);
    }
    public record Snapshot(int queueLength, long oldestAge, long lastNanos, long averageNanos, long maxNanos,
            int visitedCells, long unloadedDeferrals, boolean progressiveRescan, int lastReasons, int lastDeferrals) {}
    private static final class Metrics {
        long lastNanos, totalNanos, maxNanos, count, unloadedDeferrals; int lastVisited, lastReasons, lastDeferrals;
        void record(long nanos, int visited, CivilityDirtyQueue.Task task) {
            lastNanos = nanos; totalNanos += nanos; maxNanos = Math.max(maxNanos, nanos); count++;
            lastVisited = visited; lastReasons = task.reasons(); lastDeferrals = task.deferrals();
        }
    }
    private CivilityScanScheduler() {}
}
