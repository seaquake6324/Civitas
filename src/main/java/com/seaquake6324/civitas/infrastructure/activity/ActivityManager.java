package com.seaquake6324.civitas.infrastructure.activity;

import com.seaquake6324.civitas.application.ActivitySettlementService;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.civilization.ActivityCategory;
import com.seaquake6324.civitas.domain.civilization.ActivityRules;
import com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence;
import com.seaquake6324.civitas.domain.civilization.BoundaryPorts;
import com.seaquake6324.civitas.domain.civilization.ChunkCivilization;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import com.seaquake6324.civitas.infrastructure.civilization.CivilizationAccess;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CivilizationSavedData;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import com.seaquake6324.civitas.CivitasMod;

/** Bounded Minecraft adapter for activity attribution, transient windows, propagation and movement samples. */
public final class ActivityManager {
    private static final int MAX_WINDOWS = 4096;
    private static final ActivitySettlementService SERVICE = new ActivitySettlementService();
    private static final BoundedActivityWindowCache<Key> WINDOWS = new BoundedActivityWindowCache<>(MAX_WINDOWS);
    private static final Map<UUID, Movement> MOVEMENT = new LinkedHashMap<>();
    private static final EnumMap<Rejection, Long> REJECTIONS = new EnumMap<>(Rejection.class);
    private static final EnumMap<FacilityCategory, TagKey<Block>> FACILITY_TAGS = new EnumMap<>(FacilityCategory.class);
    static {
        for (FacilityCategory category : FacilityCategory.values()) FACILITY_TAGS.put(category,
                TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,
                        "facilities/" + category.tagPath())));
    }

    public static boolean record(ServerPlayer player, BlockPos pos, ActivityCategory category) {
        if (!(player.level() instanceof ServerLevel level)) return reject(Rejection.NO_PLAYER_SOURCE);
        String dimension = level.dimension().identifier().toString();
        long chunk = ChunkPos.pack(pos);
        Optional<City> cityAt = CitySavedData.get(level.getServer()).cityAt(dimension, chunk);
        if (cityAt.isEmpty()) return reject(Rejection.WILDERNESS);
        City city = cityAt.get();
        if (!city.isMember(player.getUUID())) return reject(Rejection.NON_MEMBER);
        if (!level.hasChunk(ChunkPos.getX(chunk), ChunkPos.getZ(chunk))) return reject(Rejection.UNLOADED_CHUNK);
        CivilizationLayer layer = CivilizationAccess.layer(level, pos);
        long start = windowStart(level.getGameTime());
        Key key = new Key(level.dimension(), city.id(), chunk, layer, start);
        ActivityWindowEvidence evidence = WINDOWS.getOrDefault(key, ActivityWindowEvidence.empty(start));
        if (evidence.has(category)) REJECTIONS.merge(Rejection.DUPLICATE_CATEGORY, 1L, Long::sum);
        put(key, evidence.record(category, player.getUUID()));
        return true;
    }

    /** A non-cancelled player interaction with a recognized working facility is one bounded category marker. */
    public static void recordFacilityUse(ServerPlayer player, BlockPos pos) {
        var state = player.level().getBlockState(pos);
        if (state.is(FACILITY_TAGS.get(FacilityCategory.FOOD))) {
            record(player, pos, ActivityCategory.LIVELIHOOD);
            return;
        }
        if (state.is(FACILITY_TAGS.get(FacilityCategory.PRODUCTION))
                || state.is(FACILITY_TAGS.get(FacilityCategory.KNOWLEDGE))
                || state.is(FACILITY_TAGS.get(FacilityCategory.STORAGE))) {
            record(player, pos, ActivityCategory.PRODUCTION);
        }
    }

    public static void sampleMovement(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)
                || player.tickCount % CivitasConfig.ACTIVITY_MOVEMENT_SAMPLE_TICKS.get() != 0) return;
        BlockPos pos = player.blockPosition();
        int size = CivitasConfig.ACTIVITY_MOVEMENT_CELL_SIZE.get();
        long cell = BlockPos.asLong(Math.floorDiv(pos.getX(), size), Math.floorDiv(pos.getY(), size), Math.floorDiv(pos.getZ(), size));
        CivilizationLayer layer = CivilizationAccess.layer(level, pos);
        String dimension = level.dimension().identifier().toString();
        Optional<City> city = CitySavedData.get(level.getServer()).cityAt(dimension, ChunkPos.pack(pos))
                .filter(value -> value.isMember(player.getUUID()));
        Movement previous = MOVEMENT.get(player.getUUID());
        if (city.isEmpty()) {
            MOVEMENT.remove(player.getUUID());
            return;
        }
        Movement next = new Movement(level.dimension(), city.get().id(), cell, layer, pos.getX(), pos.getY(), pos.getZ());
        if (previous == null || !previous.dimension.equals(next.dimension) || !previous.city.equals(next.city)
                || previous.layer != next.layer) {
            MOVEMENT.put(player.getUUID(), next);
            return;
        }
        if (hasCrossedMovementCell(previous.cell, next.cell, previous.x, previous.y, previous.z,
                pos.getX(), pos.getY(), pos.getZ(), size)) {
            if (record(player, pos, ActivityCategory.TRANSIT)) MOVEMENT.put(player.getUUID(), next);
        }
    }

    public static void remove(ServerPlayer player) { MOVEMENT.remove(player.getUUID()); }
    public static void rejectNoPlayerSource() { reject(Rejection.NO_PLAYER_SOURCE); }

    public static void tick(MinecraftServer server) {
        long windowTicks = CivitasConfig.ACTIVITY_WINDOW_TICKS.get();
        ArrayList<Map.Entry<Key, ActivityWindowEvidence>> expired = new ArrayList<>();
        for (Map.Entry<Key, ActivityWindowEvidence> entry : WINDOWS.entries()) {
            ServerLevel level = server.getLevel(entry.getKey().dimension);
            if (level != null && level.getGameTime() - entry.getKey().windowStart >= windowTicks) expired.add(entry);
        }
        if (expired.isEmpty()) return;

        // Compute only direct sources first; propagated-only windows never create another hop.
        for (Map.Entry<Key, ActivityWindowEvidence> entry : expired) {
            Key sourceKey = entry.getKey();
            ActivityWindowEvidence source = entry.getValue();
            double direct = ActivityRules.directGain(source.categoryCount(), source.contributorCount(),
                    CivitasConfig.ACTIVITY_GAIN_PER_CATEGORY.get(), CivitasConfig.ACTIVITY_LOCAL_GAIN_CAP.get());
            if (direct <= 0) continue;
            ServerLevel level = server.getLevel(sourceKey.dimension);
            if (level != null) propagate(level, sourceKey, direct);
        }

        ArrayList<Key> settled = new ArrayList<>();
        for (Map.Entry<Key, ActivityWindowEvidence> entry : WINDOWS.entries()) {
            Key key = entry.getKey();
            ServerLevel level = server.getLevel(key.dimension);
            if (level == null || level.getGameTime() - key.windowStart < windowTicks) continue;
            CivilizationSavedData data = CivilizationSavedData.get(server);
            String dimension = key.dimension.identifier().toString();
            ChunkCivilization state = data.get(dimension, key.chunk, key.layer);
            ActivitySettlementService.Result result = SERVICE.settle(state, entry.getValue(), level.getGameTime(), settings());
            data.put(dimension, key.chunk, key.layer, result.state());
            settled.add(key);
        }
        settled.forEach(WINDOWS::remove);
    }

    private static void propagate(ServerLevel level, Key source, double direct) {
        City city = CitySavedData.get(level.getServer()).byId(source.city).orElse(null);
        if (city == null) return;
        CivilizationSavedData data = CivilizationSavedData.get(level.getServer());
        ChunkCivilization sourceState = data.get(source.dimension.identifier().toString(), source.chunk, source.layer);
        int x = ChunkPos.getX(source.chunk), z = ChunkPos.getZ(source.chunk);
        for (BoundaryPorts.Direction direction : BoundaryPorts.Direction.values()) {
            int nx = x + (direction == BoundaryPorts.Direction.EAST ? 1 : direction == BoundaryPorts.Direction.WEST ? -1 : 0);
            int nz = z + (direction == BoundaryPorts.Direction.SOUTH ? 1 : direction == BoundaryPorts.Direction.NORTH ? -1 : 0);
            long neighborChunk = ChunkCoordinate.pack(nx, nz);
            if (!city.territory().contains(neighborChunk) || !level.hasChunk(nx, nz)) continue;
            ChunkCivilization neighbor = data.get(source.dimension.identifier().toString(), neighborChunk, source.layer);
            if (!sourceState.boundaryPorts().connects(direction, neighbor.boundaryPorts())) continue;
            double addition = ActivityRules.propagatedGain(direct, CivitasConfig.ACTIVITY_PROPAGATION_RATIO.get());
            Key target = new Key(source.dimension, source.city, neighborChunk, source.layer, source.windowStart);
            ActivityWindowEvidence evidence = WINDOWS.getOrDefault(target, ActivityWindowEvidence.empty(source.windowStart));
            double capped = ActivityRules.capPropagated(evidence.propagatedGain(), addition,
                    CivitasConfig.ACTIVITY_PROPAGATION_CAP.get());
            put(target, evidence.propagate(capped - evidence.propagatedGain()));
        }
    }

    public static Snapshot snapshot(ServerLevel level, BlockPos pos) {
        long chunk = ChunkPos.pack(pos);
        CivilizationLayer layer = CivilizationAccess.layer(level, pos);
        Optional<City> city = CitySavedData.get(level.getServer()).cityAt(level.dimension().identifier().toString(), chunk);
        long start = windowStart(level.getGameTime());
        ActivityWindowEvidence evidence = city.map(value -> WINDOWS.get(new Key(level.dimension(), value.id(), chunk, layer, start)))
                .orElse(null);
        int propagationMask = city.map(value -> propagationMask(level, value, chunk, layer)).orElse(0);
        return new Snapshot(evidence == null ? ActivityWindowEvidence.empty(start) : evidence,
                Map.copyOf(REJECTIONS), WINDOWS.size(), propagationMask);
    }

    public static double debugSettleCurrentWindow(ServerLevel level,BlockPos pos){
        long chunk=ChunkPos.pack(pos);CivilizationLayer layer=CivilizationAccess.layer(level,pos);Optional<City> city=CitySavedData.get(level.getServer()).cityAt(level.dimension().identifier().toString(),chunk);if(city.isEmpty())return 0;
        Key key=new Key(level.dimension(),city.get().id(),chunk,layer,windowStart(level.getGameTime()));ActivityWindowEvidence evidence=WINDOWS.get(key);if(evidence==null)return 0;
        CivilizationSavedData data=CivilizationSavedData.get(level.getServer());ChunkCivilization state=data.get(level.dimension().identifier().toString(),chunk,layer);var result=SERVICE.settle(state,evidence,level.getGameTime(),settings());data.put(level.dimension().identifier().toString(),chunk,layer,result.state());WINDOWS.remove(key);return result.directGain()+Math.min(evidence.propagatedGain(),CivitasConfig.ACTIVITY_PROPAGATION_CAP.get());
    }

    private static int propagationMask(ServerLevel level, City city, long chunk, CivilizationLayer layer) {
        CivilizationSavedData data = CivilizationSavedData.get(level.getServer());
        String dimension = level.dimension().identifier().toString();
        ChunkCivilization source = data.get(dimension, chunk, layer);
        int x = ChunkPos.getX(chunk), z = ChunkPos.getZ(chunk), mask = 0;
        for (BoundaryPorts.Direction direction : BoundaryPorts.Direction.values()) {
            int nx = x + (direction == BoundaryPorts.Direction.EAST ? 1 : direction == BoundaryPorts.Direction.WEST ? -1 : 0);
            int nz = z + (direction == BoundaryPorts.Direction.SOUTH ? 1 : direction == BoundaryPorts.Direction.NORTH ? -1 : 0);
            long neighborChunk = ChunkCoordinate.pack(nx, nz);
            if (!city.territory().contains(neighborChunk) || !level.hasChunk(nx, nz)) continue;
            ChunkCivilization neighbor = data.get(dimension, neighborChunk, layer);
            if (source.boundaryPorts().connects(direction, neighbor.boundaryPorts())) mask |= 1 << direction.ordinal();
        }
        return mask;
    }

    public static ChunkCivilization applyDecay(ServerLevel level, long chunk, CivilizationLayer layer) {
        CivilizationSavedData data = CivilizationSavedData.get(level.getServer());
        String dimension = level.dimension().identifier().toString();
        ChunkCivilization current = data.get(dimension, chunk, layer);
        ChunkCivilization next = SERVICE.decay(current, level.getGameTime(), settings());
        if (!next.equals(current)) data.put(dimension, chunk, layer, next);
        return next;
    }

    private static ActivitySettlementService.Settings settings() {
        return new ActivitySettlementService.Settings(CivitasConfig.ACTIVITY_WINDOW_TICKS.get(),
                CivitasConfig.ACTIVITY_GRACE_TICKS.get(), CivitasConfig.ACTIVITY_DECAY_PER_WINDOW.get(),
                CivitasConfig.ACTIVITY_GAIN_PER_CATEGORY.get(), CivitasConfig.ACTIVITY_PROPAGATION_RATIO.get(),
                CivitasConfig.ACTIVITY_PROPAGATION_CAP.get(), CivitasConfig.ACTIVITY_LOCAL_GAIN_CAP.get());
    }

    private static long windowStart(long now) { long size = CivitasConfig.ACTIVITY_WINDOW_TICKS.get(); return now - Math.floorMod(now, size); }
    private static void put(Key key, ActivityWindowEvidence value) {
        WINDOWS.put(key, value);
    }
    private static boolean reject(Rejection reason) { REJECTIONS.merge(reason, 1L, Long::sum); return false; }
    static boolean hasCrossedMovementCell(long previousCell, long nextCell, int previousX, int previousY, int previousZ,
            int nextX, int nextY, int nextZ, int size) {
        int distance = Math.max(Math.abs(nextX - previousX),
                Math.max(Math.abs(nextY - previousY), Math.abs(nextZ - previousZ)));
        return previousCell != nextCell && distance >= size;
    }

    public enum Rejection { NON_MEMBER, WILDERNESS, NO_PLAYER_SOURCE, DUPLICATE_CATEGORY, UNLOADED_CHUNK, LAYER_MISMATCH }
    public record Snapshot(ActivityWindowEvidence evidence, Map<Rejection, Long> rejections, int cachedWindows,
            int propagationMask) {}
    private record Key(ResourceKey<Level> dimension, UUID city, long chunk, CivilizationLayer layer, long windowStart) {}
    private record Movement(ResourceKey<Level> dimension, UUID city, long cell, CivilizationLayer layer,
            int x, int y, int z) {}
    private ActivityManager() {}
}
