package com.seaquake6324.civitas.infrastructure.civilization;

import com.seaquake6324.civitas.CivitasMod;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.civilization.BoundaryPorts;
import com.seaquake6324.civitas.domain.civilization.CivilityEvidence;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import java.util.ArrayList;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/** Minecraft fact collector using a fixed 2x2 horizontal sample and an explicit visit cap. */
public final class BoundedCivilityScanner {
    public static final int HORIZONTAL_STEP = 4;
    public static final int VERTICAL_STEP = 1;
    public static final int MAX_VISITED_CELLS = 2048;
    private static final EnumMap<FacilityCategory, TagKey<Block>> FACILITY_TAGS = new EnumMap<>(FacilityCategory.class);
    static {
        for (FacilityCategory category : FacilityCategory.values()) FACILITY_TAGS.put(category,
                TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(CivitasMod.MOD_ID,
                        "facilities/" + category.tagPath())));
    }

    public ScanResult scan(ServerLevel level, City city, long packedChunk, CivilizationLayer layer,
            NeighborPorts neighbors) {
        ChunkCoordinate coordinate = ChunkCoordinate.unpack(packedChunk);
        ChunkPos chunk = new ChunkPos(coordinate.x(), coordinate.z());
        if (!city.ownsChunk(level.dimension().identifier().toString(), packedChunk)
                || !level.hasChunk(chunk.x(), chunk.z())) return null;
        int minY = level.getMinY();
        int maxY = level.getMaxY() - 2;
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING, chunk.getMiddleBlockX(), chunk.getMiddleBlockZ());
        int scanMin = layer == CivilizationLayer.SURFACE ? Math.max(minY, surface - 8) : minY;
        int scanMax = layer == CivilizationLayer.SURFACE ? Math.min(maxY, surface + 24) : Math.min(maxY, surface - 9);
        Mutable facts = new Mutable();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        List<Cell> passable = new ArrayList<>();
        EnumMap<FacilityCategory, Set<Long>> facilityCells = new EnumMap<>(FacilityCategory.class);
        for (FacilityCategory category : FacilityCategory.values()) facilityCells.put(category, new HashSet<>());

        outer: for (int y = scanMin; y <= scanMax; y += VERTICAL_STEP) {
            for (int x = chunk.getMinBlockX(); x <= chunk.getMaxBlockX(); x += HORIZONTAL_STEP) {
                for (int z = chunk.getMinBlockZ(); z <= chunk.getMaxBlockZ(); z += HORIZONTAL_STEP) {
                    if (++facts.visited > MAX_VISITED_CELLS) break outer;
                    pos.set(x, y, z);
                    if (!passable(level, pos) || !level.getBlockState(pos.below()).blocksMotion()) continue;
                    facts.passable++;
                    passable.add(new Cell(x, y, z));
                    facts.standable++;
                    int enclosed = 0;
                    for (Direction direction : Direction.values()) if (level.getBlockState(pos.relative(direction)).blocksMotion()) enclosed++;
                    facts.enclosureSum += enclosed / 6.0;
                    boolean hazardous = false;
                    for (Direction direction : Direction.values()) {
                        BlockPos adjacent = pos.relative(direction);
                        BlockState state = level.getBlockState(adjacent);
                        if (isHazard(state)) { facts.hazards++; hazardous = true; if (direction != Direction.UP && level.getBlockState(adjacent.above()).blocksMotion()) facts.protectedHazards++; }
                        for (FacilityCategory category : FacilityCategory.values()) if (state.is(FACILITY_TAGS.get(category)))
                            facilityCells.get(category).add(cellKey(pos));
                    }
                    if (!hazardous) facts.safe++;
                    addPort(facts, chunk, x, y, z);
                }
            }
        }
        int totalFacilities = facilityCells.values().stream().mapToInt(Set::size).sum();
        List<Integer> distributions = facilityCells.values().stream().map(set -> Math.min(3, set.size())).toList();
        int territoryEdges = 0, connectedEdges = 0;
        for (BoundaryPorts.Direction direction : BoundaryPorts.Direction.values()) {
            if (!neighbors.claimed(direction)) continue;
            territoryEdges++;
            if (facts.ports().connects(direction, neighbors.ports(direction))) connectedEdges++;
        }
        boolean core = packedChunk == city.coreChunk();
        int largestConnected = largestComponent(passable);
        CivilityEvidence evidence = new CivilityEvidence(facts.visited, facts.standable, facts.passable,
                largestConnected, facts.passable == 0 ? 0 : facts.enclosureSum / facts.passable,
                facts.safe, facts.hazards, facts.protectedHazards, distributions, totalFacilities,
                territoryEdges, connectedEdges, core, !core || facts.passable > 0, facts.ports());
        return new ScanResult(fingerprint(evidence), evidence);
    }

    private static boolean passable(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty();
    }
    private static String fingerprint(CivilityEvidence evidence) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(evidence.toString().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 12);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
    private static boolean isHazard(BlockState state) {
        return state.is(Blocks.LAVA) || state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)
                || state.is(Blocks.POWDER_SNOW) || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK);
    }
    private static long cellKey(BlockPos pos) { return BlockPos.asLong(pos.getX() >> 2, pos.getY() >> 2, pos.getZ() >> 2); }
    private static int largestComponent(List<Cell> cells) {
        Set<Cell> remaining = new HashSet<>(cells);
        int largest = 0;
        ArrayDeque<Cell> queue = new ArrayDeque<>();
        while (!remaining.isEmpty()) {
            Cell start = remaining.iterator().next();
            remaining.remove(start); queue.add(start); int size = 0;
            while (!queue.isEmpty()) {
                Cell cell = queue.removeFirst(); size++;
                for (Cell neighbor : cell.neighbors()) if (remaining.remove(neighbor)) queue.addLast(neighbor);
            }
            largest = Math.max(largest, size);
        }
        return largest;
    }
    private static void addPort(Mutable f, ChunkPos c, int x, int y, int z) {
        int bit = 1 << Math.min(15, Math.max(0, (y & 12) + ((x + z) & 3)));
        if (z == c.getMinBlockZ()) f.north |= bit;
        if (x >= c.getMaxBlockX() - 1) f.east |= bit;
        if (z >= c.getMaxBlockZ() - 1) f.south |= bit;
        if (x == c.getMinBlockX()) f.west |= bit;
    }
    private static final class Mutable {
        int visited, standable, passable, safe, hazards, protectedHazards;
        int north, east, south, west; double enclosureSum;
        BoundaryPorts ports() { return new BoundaryPorts(north, east, south, west); }
    }
    private record Cell(int x, int y, int z) {
        List<Cell> neighbors() {
            return List.of(new Cell(x + HORIZONTAL_STEP,y,z), new Cell(x - HORIZONTAL_STEP,y,z),
                    new Cell(x,y,z + HORIZONTAL_STEP), new Cell(x,y,z - HORIZONTAL_STEP),
                    new Cell(x,y + VERTICAL_STEP,z), new Cell(x,y - VERTICAL_STEP,z));
        }
    }
    public record ScanResult(String fingerprint, CivilityEvidence evidence) {}
    public interface NeighborPorts {
        boolean claimed(BoundaryPorts.Direction direction);
        BoundaryPorts ports(BoundaryPorts.Direction direction);
    }
}
