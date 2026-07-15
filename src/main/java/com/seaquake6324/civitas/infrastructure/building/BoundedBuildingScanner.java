package com.seaquake6324.civitas.infrastructure.building;

import com.seaquake6324.civitas.CivitasMod;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.BuildingValidation;
import com.seaquake6324.civitas.domain.building.BuildingFeatures;
import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.state.BlockState;

/** Main-thread adapter that takes a bounded world snapshot and emits pure validation evidence. */
public final class BoundedBuildingScanner {
    private static final EnumMap<FacilityCategory, TagKey<net.minecraft.world.level.block.Block>> FACILITY_TAGS =
            new EnumMap<>(FacilityCategory.class);
    static {
        for (FacilityCategory category : FacilityCategory.values()) FACILITY_TAGS.put(category,
                TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,
                        "facilities/" + category.tagPath())));
    }

    public BuildingValidation.Evidence scan(ServerLevel level, City city, BlockPos entrance, BlockPos selectedInterior) {
        long started = System.nanoTime();
        boolean entranceValid = entrance.distManhattan(selectedInterior) <= 16
                && (level.getBlockState(entrance).getBlock() instanceof DoorBlock
                || level.getBlockState(entrance).getBlock() instanceof FenceGateBlock);
        BlockPos interior = normalizeInterior(level, selectedInterior);
        boolean interiorValid = standable(level, interior);
        int limit = CivitasConfig.BUILDING_SCAN_CELL_LIMIT.get();
        int queueLimit = Math.max(1, Math.multiplyExact(limit, 6));
        int horizontal = CivitasConfig.BUILDING_SCAN_HORIZONTAL_RADIUS.get();
        int vertical = CivitasConfig.BUILDING_SCAN_VERTICAL_RADIUS.get();
        Set<Long> cells = new HashSet<>();
        Set<Long> boundaryPorts = new HashSet<>();
        Map<FacilityCategory, Set<Long>> facilityBlocks = new EnumMap<>(FacilityCategory.class);
        for (FacilityCategory category : FacilityCategory.values()) facilityBlocks.put(category, new HashSet<>());
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        Set<Long> queued = new HashSet<>();
        boolean territoryValid = true, openBoundary = false, limitReached = false, evidenceComplete = true;
        int visited = 0, queuePeak = 0;
        if (interiorValid) { queue.add(interior.immutable()); queued.add(interior.asLong()); }
        queuePeak = queue.size();
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (++visited > limit) { limitReached = true; break; }
            if (pos.equals(entrance) || pos.equals(entrance.above())) continue;
            int dx = Math.abs(pos.getX() - interior.getX()), dz = Math.abs(pos.getZ() - interior.getZ());
            int dy = Math.abs(pos.getY() - interior.getY());
            if (dx > horizontal || dz > horizontal || dy > vertical) { openBoundary = true; continue; }
            ChunkPos chunk = ChunkPos.containing(pos);
            if (!city.ownsChunk(level.dimension().identifier().toString(), ChunkPos.pack(chunk.x(), chunk.z()))) {
                territoryValid = false;
                continue;
            }
            if (!level.hasChunk(chunk.x(), chunk.z())) { evidenceComplete = false; continue; }
            if (!standable(level, pos)) continue;
            cells.add(pos.asLong());
            if (!collectFeatures(level, pos, facilityBlocks, boundaryPorts)) evidenceComplete = false;
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (queued.contains(next.asLong())) continue;
                if (queued.size() >= queueLimit) { limitReached = true; continue; }
                queued.add(next.asLong());queue.addLast(next.immutable());queuePeak=Math.max(queuePeak,queue.size());
            }
        }
        if (!queue.isEmpty()) limitReached = true;
        EnumMap<FacilityCategory, Integer> facilities = new EnumMap<>(FacilityCategory.class);
        facilityBlocks.forEach((category, blocks) -> facilities.put(category, blocks.size()));
        boolean entranceConnected = entranceValid && connectedEntrance(level, entrance, cells);
        entranceValid = entranceValid && entranceConnected;
        if (entranceValid) boundaryPorts.add(entrance.asLong());
        BuildingFeatures features = new BuildingFeatures(boundaryPorts,
                facilityBlocks.get(FacilityCategory.PRODUCTION),
                facilityBlocks.get(FacilityCategory.STORAGE),
                entranceConnected);
        return new BuildingValidation.Evidence(visited, cells.size(), entranceValid, interiorValid,
                territoryValid, openBoundary, limitReached, facilities, cells, features, evidenceComplete, queuePeak,
                Math.max(0, (System.nanoTime() - started) / 1_000));
    }

    private static BlockPos normalizeInterior(ServerLevel level, BlockPos selected) {
        if (standable(level, selected)) return selected.immutable();
        if (standable(level, selected.above())) return selected.above().immutable();
        return selected.immutable();
    }
    private static boolean standable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getBlockState(pos.below()).blocksMotion();
    }
    private static boolean collectFeatures(ServerLevel level, BlockPos cell,
            Map<FacilityCategory, Set<Long>> facilities, Set<Long> boundaryPorts) {
        for (Direction direction : Direction.values()) {
            BlockPos at = cell.relative(direction);
            ChunkPos chunk = ChunkPos.containing(at);
            if (!level.hasChunk(chunk.x(), chunk.z())) return false;
            BlockState state = level.getBlockState(at);
            if (state.getBlock() instanceof DoorBlock || state.getBlock() instanceof FenceGateBlock) boundaryPorts.add(at.asLong());
            for (FacilityCategory category : FacilityCategory.values())
                if (state.is(FACILITY_TAGS.get(category))) facilities.get(category).add(at.asLong());
        }
        return true;
    }
    private static boolean connectedEntrance(ServerLevel level, BlockPos entrance, Set<Long> cells) {
        for (BlockPos anchor : java.util.List.of(entrance, entrance.below())) {
            for (Direction direction : Direction.Plane.HORIZONTAL) {
                BlockPos inside = anchor.relative(direction), outside = anchor.relative(direction.getOpposite());
                if (!cells.contains(inside.asLong()) || cells.contains(outside.asLong())) continue;
                ChunkPos chunk = ChunkPos.containing(outside);
                if (level.hasChunk(chunk.x(), chunk.z()) && standable(level, outside)) return true;
            }
        }
        return false;
    }
}
