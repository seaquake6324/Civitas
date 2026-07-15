package com.seaquake6324.civitas.infrastructure.security;

import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.building.BuildingRecord;
import com.seaquake6324.civitas.domain.building.BuildingStatus;
import com.seaquake6324.civitas.domain.security.InfiltrationSource;
import com.seaquake6324.civitas.domain.territory.TerritoryTopology;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.levelgen.Heightmap;

/** Bounded main-thread world adapter for legal infiltration evidence. Never force-loads a chunk. */
public final class InfiltrationCellScanner {
    public Result scan(ServerLevel level, City city, long chunk, List<BuildingRecord> records,
            boolean buildingRecordsComplete, int sampleCap, int candidateCap, double guardedThreshold,
            double patrolCoverage, double guardResponse) {
        ChunkCoordinate coordinate = ChunkCoordinate.unpack(chunk);
        ChunkPos cp = new ChunkPos(coordinate.x(), coordinate.z());
        BlockPos center = new BlockPos(cp.getMiddleBlockX(), level.getMinY(), cp.getMiddleBlockZ());
        if (!level.hasChunkAt(center)) return new Result(List.of(), 0, 0, false, false, 0, false);
        int samples = 0, hardSamples = Math.max(1, sampleCap), hardCandidates = Math.max(1, candidateCap);
        boolean truncated = false;
        ArrayList<ObservedSource> candidates = new ArrayList<>();
        double maxDarkness = 0;

        InfiltrationSource entrySource = Math.max(patrolCoverage, guardResponse) >= guardedThreshold
                ? InfiltrationSource.DISGUISED_ENTRY : InfiltrationSource.UNCONTROLLED_ENTRANCE;
        for (BuildingRecord record : records) {
            if (record.status() != BuildingStatus.VALID || !record.features().entranceConnected()) continue;
            if (samples >= hardSamples || candidates.size() >= hardCandidates) { truncated = true; break; }
            samples++;
            BlockPos entrance = BlockPos.of(record.entrance());
            BlockPos spawn = adjacentStandable(level, entrance, records);
            if (spawn != null) candidates.add(new ObservedSource(entrySource, spawn.asLong(), chunk, darkness(level, spawn)));
        }

        for (var edge : TerritoryTopology.borderEdges(city.territory(), chunk)) {
            Direction direction = direction(edge);
            int edgeSamples = CivitasConfig.INFILTRATION_BORDER_SAMPLES_PER_EDGE.get();
            for (int i = 0; i < edgeSamples; i++) {
                if (samples >= hardSamples || candidates.size() >= hardCandidates) { truncated = true; break; }
                int along = Math.max(1, Math.min(14, (i + 1) * 16 / (edgeSamples + 1)));
                int x = direction.getAxis() == Direction.Axis.X ? (direction == Direction.EAST ? cp.getMaxBlockX() : cp.getMinBlockX()) : cp.getMinBlockX() + along;
                int z = direction.getAxis() == Direction.Axis.Z ? (direction == Direction.SOUTH ? cp.getMaxBlockZ() : cp.getMinBlockZ()) : cp.getMinBlockZ() + along;
                int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
                BlockPos inside = new BlockPos(x, y, z), outside = inside.relative(direction);
                samples += 6;
                if (level.hasChunkAt(outside) && standable(level, inside) && standable(level, outside)
                        && !port(level, inside) && !port(level, outside) && flankedByBarrier(level, inside, direction)
                        && !insideBuilding(inside, records))
                    candidates.add(new ObservedSource(InfiltrationSource.WALL_GAP, outside.asLong(), chunk, darkness(level, outside)));
            }
        }

        int columns = Math.max(1, Math.min(CivitasConfig.INFILTRATION_UNDERGROUND_COLUMNS_PER_CELL.get(), hardSamples / 8));
        for (int i = 0; i < columns; i++) {
            if (samples >= hardSamples || candidates.size() >= hardCandidates) { truncated = true; break; }
            int x = cp.getMinBlockX() + 2 + (i * 7 % 12), z = cp.getMinBlockZ() + 2 + (i * 11 % 12);
            int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            int bottom = Math.max(level.getMinY() + 1, surface - CivitasConfig.INFILTRATION_VERTICAL_DEPTH.get());
            for (int y = surface - 3; y >= bottom; y--) {
                if (samples++ >= hardSamples) { truncated = true; break; }
                BlockPos at = new BlockPos(x, y, z);
                if (!standable(level, at) || insideBuilding(at, records) || level.canSeeSky(at.above())) continue;
                double dark = darkness(level, at); maxDarkness = Math.max(maxDarkness, dark);
                if (dark <= 0) continue;
                InfiltrationSource source = caveMouth(level, at) ? InfiltrationSource.CAVE : InfiltrationSource.UNDERGROUND_DARKNESS;
                candidates.add(new ObservedSource(source, at.asLong(), chunk, dark));
                break;
            }
        }
        if (candidates.size() >= hardCandidates) truncated = true;
        double access = candidates.isEmpty() ? 0 : 100;
        boolean complete = buildingRecordsComplete && !truncated;
        return new Result(List.copyOf(candidates), access, maxDarkness, complete, complete, samples, truncated);
    }

    public boolean revalidate(ServerLevel level, long position, List<BuildingRecord> records) {
        BlockPos at = BlockPos.of(position);
        return level.hasChunkAt(at) && standable(level, at) && !insideBuilding(at, records);
    }

    private static BlockPos adjacentStandable(ServerLevel level, BlockPos entrance, List<BuildingRecord> records) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos at = entrance.relative(direction);
            if (level.hasChunkAt(at) && standable(level, at) && !insideBuilding(at, records)) return at;
            at = at.below();
            if (level.hasChunkAt(at) && standable(level, at) && !insideBuilding(at, records)) return at;
        }
        return null;
    }

    private static boolean standable(ServerLevel level, BlockPos at) {
        return level.isInWorldBounds(at) && level.getBlockState(at).getCollisionShape(level, at).isEmpty()
                && level.getBlockState(at.above()).getCollisionShape(level, at.above()).isEmpty()
                && level.getBlockState(at.below()).blocksMotion() && level.getFluidState(at).isEmpty()
                && level.getFluidState(at.above()).isEmpty();
    }

    private static boolean insideBuilding(BlockPos pos, List<BuildingRecord> records) {
        long packed = pos.asLong();
        return records.stream().anyMatch(r -> r.status() == BuildingStatus.VALID && r.cells().contains(packed));
    }

    private static boolean caveMouth(ServerLevel level, BlockPos at) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            BlockPos near = at.relative(direction, CivitasConfig.INFILTRATION_CAVE_MOUTH_DISTANCE.get());
            if (level.hasChunkAt(near) && level.canSeeSky(near.above())) return true;
        }
        return false;
    }

    private static double darkness(ServerLevel level, BlockPos at) {
        int light = Math.max(level.getBrightness(LightLayer.BLOCK, at), level.getBrightness(LightLayer.SKY, at));
        return Math.max(0, Math.min(100, (15 - light) * (100.0 / 15.0)));
    }

    private static boolean port(ServerLevel level, BlockPos pos) {
        var block = level.getBlockState(pos).getBlock(); return block instanceof DoorBlock || block instanceof FenceGateBlock;
    }
    private static boolean flankedByBarrier(ServerLevel level, BlockPos gap, Direction outward) {
        Direction tangent = outward.getAxis() == Direction.Axis.X ? Direction.NORTH : Direction.EAST;
        BlockPos a = gap.relative(tangent), b = gap.relative(tangent.getOpposite());
        return barrier(level, a) && barrier(level, b);
    }
    private static boolean barrier(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).blocksMotion() || level.getBlockState(pos.above()).blocksMotion();
    }

    private static Direction direction(TerritoryTopology.Direction direction) { return switch (direction) {
        case NORTH -> Direction.NORTH; case SOUTH -> Direction.SOUTH; case EAST -> Direction.EAST; case WEST -> Direction.WEST;
    }; }

    public record ObservedSource(InfiltrationSource source, long position, long chunk, double darkness) {}
    public record Result(List<ObservedSource> candidates, double infiltrationAccess, double undergroundDarkness,
            boolean infiltrationEvidenceLinked, boolean undergroundEvidenceLinked, int worldSamples, boolean truncated) {}
}
