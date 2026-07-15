package com.seaquake6324.civitas.infrastructure.building;

import com.seaquake6324.civitas.application.RevalidateBuildingService;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.BuildingRecord;
import com.seaquake6324.civitas.domain.building.BuildingStatus;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.ThreatSavedData;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/** Bounded main-thread queue. It never loads chunks and never retains world objects. */
public final class BuildingRevalidationManager {
    private static final LinkedHashSet<UUID> QUEUE = new LinkedHashSet<>();
    private static final BoundedBuildingScanner SCANNER = new BoundedBuildingScanner();
    private static final RevalidateBuildingService SERVICE = new RevalidateBuildingService();
    private static long attempted, restored, stillInvalid, deferred, dropped, elapsedMicros;
    private static int queuePeak;
    private static long nextSweep;

    public static void invalidate(ServerLevel level, BlockPos changed) {
        BuildingSavedData data = BuildingSavedData.get(level.getServer());
        var stale=data.markStaleRecordsAt(level.dimension().identifier().toString(), changed);enqueue(stale);ThreatSavedData threat=ThreatSavedData.get(level.getServer());for(UUID id:stale)threat.markPatrolRoutesForPostStale(id,"guard_post_changed");
    }

    public static void tick(MinecraftServer server) {
        BuildingSavedData buildings = BuildingSavedData.get(server);
        if (server.getTickCount() >= nextSweep) {
            nextSweep = server.getTickCount() + 200;
            enqueue(buildings.recordsByStatus(BuildingStatus.STALE,CivitasConfig.BUILDING_REVALIDATION_QUEUE_LIMIT.get()).stream().map(BuildingRecord::id).toList());
        }
        int budget = CivitasConfig.BUILDING_REVALIDATION_PER_TICK.get();
        for (int i = 0; i < budget; i++) {
            UUID id = poll();
            if (id == null) break;
            BuildingRecord record = buildings.byId(id).orElse(null);
            if (record == null || record.status() != BuildingStatus.STALE) continue;
            ServerLevel level = level(server, record.dimension());
            City city = CitySavedData.get(server).byId(record.cityId()).orElse(null);
            if (level == null || city == null || !loaded(level, record)) { deferred++; continue; }
            long started = System.nanoTime();
            var evidence = SCANNER.scan(level, city, BlockPos.of(record.entrance()), BlockPos.of(record.interior()));
            if (!evidence.evidenceComplete()) { deferred++; continue; }
            var result = SERVICE.revalidate(buildings, city, record, level.getGameTime(), evidence);
            elapsedMicros += Math.max(0, (System.nanoTime() - started) / 1_000);
            attempted++;
            if (result.validation().valid()) restored++; else stillInvalid++;
        }
    }

    private static boolean loaded(ServerLevel level, BuildingRecord record) {
        BlockPos entrance = BlockPos.of(record.entrance()), interior = BlockPos.of(record.interior());
        ChunkPos a = ChunkPos.containing(entrance), b = ChunkPos.containing(interior);
        return level.hasChunk(a.x(), a.z()) && level.hasChunk(b.x(), b.z());
    }
    private static ServerLevel level(MinecraftServer server, String dimension) {
        for (ServerLevel level : server.getAllLevels())
            if (level.dimension().identifier().toString().equals(dimension)) return level;
        return null;
    }
    private static void enqueue(Iterable<UUID> ids) {
        int limit = CivitasConfig.BUILDING_REVALIDATION_QUEUE_LIMIT.get();
        for (UUID id : ids) {
            if (QUEUE.contains(id)) continue;
            if (QUEUE.size() >= limit) { dropped++; continue; }
            QUEUE.add(id);
            queuePeak=Math.max(queuePeak,QUEUE.size());
        }
    }
    private static UUID poll() {
        Iterator<UUID> iterator = QUEUE.iterator();
        if (!iterator.hasNext()) return null;
        UUID id = iterator.next(); iterator.remove(); return id;
    }
    public static Metrics metrics() {
        return new Metrics(QUEUE.size(), queuePeak, attempted, restored, stillInvalid, deferred, dropped,
                elapsedMicros, attempted == 0 ? 0 : elapsedMicros / attempted);
    }
    public record Metrics(int queued, int queuePeak, long attempted, long restored, long stillInvalid,
            long deferred, long dropped, long totalMicros, long averageMicros) {}
    private BuildingRevalidationManager() {}
}
