package com.seaquake6324.civitas.infrastructure.spawn;

import com.seaquake6324.civitas.domain.region.RegionClassificationRules;
import com.seaquake6324.civitas.domain.region.RegionDiagnostics;
import com.seaquake6324.civitas.domain.region.RegionEvidence;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.Heightmap;

/** Server-authoritative environmental sampling and bounded 3D path search. */
public final class ServerRegionClassifier {
    public static final int CELL_SIZE = 4;
    static final int MAX_CACHED_CELLS = 8192;
    static final int TIMING_WINDOW_SIZE = 128;
    private static final Map<ServerLevel, LevelCache> CACHES = new WeakHashMap<>();
    private static final int[] SAMPLE_OFFSETS = {-2, 0, 2};

    public static RegionDiagnostics classify(ServerLevel level, BlockPos pos) {
        return lookup(level, pos).diagnostics();
    }

    public static ClassificationSample classifyWithMetrics(ServerLevel level, BlockPos pos) {
        CachedClassification cached = lookup(level, pos);
        LevelCache cache = CACHES.get(level);
        return new ClassificationSample(cached.diagnostics(), cached.sampleOrigin(), cellCoordinate(pos.getX()),
                cellCoordinate(pos.getY()), cellCoordinate(pos.getZ()), cache.lastLookupHit, cache.cacheHits,
                cache.cacheMisses, cached.calculationNanos(), cache.timings.averageNanos(),
                cache.timings.maximumNanos(), cache.results.size(), cache.lastInvalidationReason);
    }

    private static CachedClassification lookup(ServerLevel level, BlockPos pos) {
        if (!level.getServer().isSameThread()) throw new IllegalStateException("Region classification must run on the server thread");
        Settings settings = Settings.read();
        LevelCache cache = CACHES.computeIfAbsent(level, ignored -> new LevelCache());
        if (!cache.settingsInitialized) {
            cache.settingsHash = settings.hashCode();
            cache.settingsInitialized = true;
        } else if (cache.settingsHash != settings.hashCode()) {
            cache.results.clear();
            cache.settingsHash = settings.hashCode();
            cache.lastInvalidationReason = CacheInvalidationReason.SETTINGS_CHANGED;
        }
        long key = cellKey(pos);
        CachedClassification cached = cache.results.getAndMoveToLast(key);
        if (cached != null) {
            cache.lastLookupHit = true;
            cache.cacheHits++;
            return cached;
        }
        cache.lastLookupHit = false;
        cache.cacheMisses++;
        long startedAt = System.nanoTime();
        RegionDiagnostics result = sampleAndClassify(level, pos, settings);
        long calculationNanos = Math.max(0L, System.nanoTime() - startedAt);
        cached = new CachedClassification(result, pos.immutable(), calculationNanos);
        cache.timings.add(calculationNanos);
        cache.results.putAndMoveToLast(key, cached);
        while (cache.results.size() > MAX_CACHED_CELLS) {
            cache.results.removeFirst();
            cache.lastInvalidationReason = CacheInvalidationReason.LRU_EVICTED;
        }
        return cached;
    }

    public static void invalidateNear(ServerLevel level, BlockPos changedPos) {
        LevelCache cache = CACHES.get(level);
        if (cache == null || cache.results.isEmpty()) return;
        int horizontalCells = ceilDiv(CivitasConfig.REGION_HORIZONTAL_RADIUS.get() + CELL_SIZE, CELL_SIZE);
        int verticalCells = ceilDiv(CivitasConfig.REGION_VERTICAL_RADIUS.get() + CELL_SIZE, CELL_SIZE);
        int cx = Math.floorDiv(changedPos.getX(), CELL_SIZE);
        int cy = Math.floorDiv(changedPos.getY(), CELL_SIZE);
        int cz = Math.floorDiv(changedPos.getZ(), CELL_SIZE);
        boolean invalidated = false;
        for (int x = cx - horizontalCells; x <= cx + horizontalCells; x++) {
            for (int y = cy - verticalCells; y <= cy + verticalCells; y++) {
                for (int z = cz - horizontalCells; z <= cz + horizontalCells; z++) {
                    if (cache.results.remove(BlockPos.asLong(x, y, z)) != null) invalidated = true;
                }
            }
        }
        if (invalidated) cache.lastInvalidationReason = CacheInvalidationReason.BLOCK_CHANGED;
    }

    public static int cachedCellCount(ServerLevel level) {
        LevelCache cache = CACHES.get(level);
        return cache == null ? 0 : cache.results.size();
    }

    private static RegionDiagnostics sampleAndClassify(ServerLevel level, BlockPos origin, Settings settings) {
        int surfaceMedian = medianSurfaceHeight(level, origin, settings.horizontalRadius);
        int skySamples = skySamples(level, origin);
        int[] coverage = coverageSamples(level, origin, surfaceMedian, settings.verticalRadius);
        double enclosure = enclosureRatio(level, origin);
        SearchResult search = searchOutdoor(level, origin, settings);
        RegionEvidence evidence = new RegionEvidence(surfaceMedian, Math.max(0, surfaceMedian - origin.getY()), skySamples,
                search.distance, search.reached, coverage[0], coverage[1], enclosure, search.visited, search.exhausted);
        return RegionClassificationRules.classify(evidence, settings.rules());
    }

    private static int medianSurfaceHeight(ServerLevel level, BlockPos origin, int radius) {
        int stride = Math.max(3, radius / 2);
        int[] heights = new int[9];
        int i = 0;
        for (int dz : new int[]{-stride, 0, stride}) {
            for (int dx : new int[]{-stride, 0, stride}) {
                int x = origin.getX() + dx;
                int z = origin.getZ() + dz;
                heights[i++] = level.hasChunkAt(new BlockPos(x, origin.getY(), z))
                        ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) : origin.getY();
            }
        }
        Arrays.sort(heights);
        return heights[4];
    }

    private static int skySamples(ServerLevel level, BlockPos origin) {
        int count = 0;
        for (int dz : SAMPLE_OFFSETS) for (int dx : SAMPLE_OFFSETS) {
            BlockPos sample = origin.offset(dx, 1, dz);
            if (level.hasChunkAt(sample) && level.canSeeSky(sample)) count++;
        }
        return count;
    }

    private static int[] coverageSamples(ServerLevel level, BlockPos origin, int surfaceMedian, int verticalRadius) {
        int[] values = new int[9];
        int i = 0;
        int top = Math.min(level.getMaxY() - 1, Math.max(surfaceMedian + 2, origin.getY() + verticalRadius));
        for (int dz : SAMPLE_OFFSETS) for (int dx : SAMPLE_OFFSETS) {
            int covered = 0;
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(origin.getX() + dx, origin.getY() + 2, origin.getZ() + dz);
            if (level.hasChunkAt(cursor)) {
                for (int y = cursor.getY(); y <= top; y++) {
                    cursor.setY(y);
                    if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) covered++;
                }
            }
            values[i++] = covered;
        }
        Arrays.sort(values);
        return new int[]{values[4], values[8]};
    }

    private static double enclosureRatio(ServerLevel level, BlockPos origin) {
        int blocked = 0;
        int total = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dy = -1; dy <= 2; dy++) for (int dz = -2; dz <= 2; dz++) for (int dx = -2; dx <= 2; dx++) {
            if (Math.abs(dx) < 2 && Math.abs(dz) < 2 && dy >= 0 && dy <= 1) continue;
            cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
            if (!level.hasChunkAt(cursor)) continue;
            total++;
            if (!level.getBlockState(cursor).getCollisionShape(level, cursor).isEmpty()) blocked++;
        }
        return total == 0 ? 1.0 : (double)blocked / total;
    }

    private static SearchResult searchOutdoor(ServerLevel level, BlockPos origin, Settings settings) {
        BlockPos start = findPassableStart(level, origin);
        if (start == null) return new SearchResult(false, Integer.MAX_VALUE, 0, false);
        ArrayDeque<SearchNode> queue = new ArrayDeque<>();
        LongOpenHashSet visited = new LongOpenHashSet(settings.nodeLimit * 2);
        queue.add(new SearchNode(start.immutable(), 0));
        visited.add(start.asLong());
        int visitedCount = 0;
        while (!queue.isEmpty() && visitedCount < settings.nodeLimit) {
            SearchNode node = queue.removeFirst();
            visitedCount++;
            if (isOutdoor(level, node.pos)) return new SearchResult(true, node.distance, visitedCount, false);
            for (Direction direction : Direction.values()) {
                BlockPos next = node.pos.relative(direction);
                if (Math.abs(next.getX() - origin.getX()) > settings.horizontalRadius
                        || Math.abs(next.getZ() - origin.getZ()) > settings.horizontalRadius
                        || Math.abs(next.getY() - origin.getY()) > settings.verticalRadius
                        || !level.isInWorldBounds(next) || !level.hasChunkAt(next)
                        || !visited.add(next.asLong()) || !isPassable(level, next)) continue;
                queue.addLast(new SearchNode(next, node.distance + 1));
            }
        }
        return new SearchResult(false, Integer.MAX_VALUE, visitedCount, !queue.isEmpty());
    }

    private static BlockPos findPassableStart(ServerLevel level, BlockPos origin) {
        if (isPassable(level, origin)) return origin;
        if (isPassable(level, origin.above())) return origin.above();
        if (isPassable(level, origin.below())) return origin.below();
        return null;
    }

    private static boolean isPassable(ServerLevel level, BlockPos feet) {
        if (!level.getFluidState(feet).isEmpty()) return false;
        return level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                && level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty();
    }

    private static boolean isOutdoor(ServerLevel level, BlockPos pos) {
        if (!level.canSeeSky(pos.above())) return false;
        int surface = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, pos.getX(), pos.getZ());
        return pos.getY() >= surface - 3;
    }

    private static long cellKey(BlockPos pos) {
        return BlockPos.asLong(cellCoordinate(pos.getX()), cellCoordinate(pos.getY()), cellCoordinate(pos.getZ()));
    }

    static int cellCoordinate(int blockCoordinate) { return Math.floorDiv(blockCoordinate, CELL_SIZE); }
    private static int ceilDiv(int value, int divisor) { return (value + divisor - 1) / divisor; }
    private record SearchNode(BlockPos pos, int distance) {}
    private record SearchResult(boolean reached, int distance, int visited, boolean exhausted) {}
    private record CachedClassification(RegionDiagnostics diagnostics, BlockPos sampleOrigin, long calculationNanos) {}
    private static final class LevelCache {
        private final Long2ObjectLinkedOpenHashMap<CachedClassification> results = new Long2ObjectLinkedOpenHashMap<>();
        private final RollingTimingWindow timings = new RollingTimingWindow(TIMING_WINDOW_SIZE);
        private int settingsHash;
        private boolean settingsInitialized;
        private boolean lastLookupHit;
        private long cacheHits;
        private long cacheMisses;
        private CacheInvalidationReason lastInvalidationReason = CacheInvalidationReason.NONE;
    }

    public enum CacheInvalidationReason {
        NONE,
        SETTINGS_CHANGED,
        BLOCK_CHANGED,
        LRU_EVICTED
    }

    public record ClassificationSample(RegionDiagnostics diagnostics, BlockPos sampleOrigin,
            int cellX, int cellY, int cellZ, boolean cacheHit, long cacheHits, long cacheMisses,
            long lastCalculationNanos, long rollingAverageNanos, long rollingMaximumNanos,
            int cachedCells, CacheInvalidationReason lastInvalidationReason) {}

    static final class RollingTimingWindow {
        private final long[] values;
        private int nextIndex;
        private int size;
        private long sum;
        private long maximum;

        RollingTimingWindow(int capacity) {
            if (capacity <= 0) throw new IllegalArgumentException("Timing window capacity must be positive");
            values = new long[capacity];
        }

        void add(long nanos) {
            long value = Math.max(0L, nanos);
            boolean recomputeMaximum = size == values.length && values[nextIndex] == maximum;
            if (size == values.length) {
                sum -= values[nextIndex];
            } else {
                size++;
            }
            values[nextIndex] = value;
            sum += value;
            nextIndex = (nextIndex + 1) % values.length;
            if (recomputeMaximum) {
                maximum = 0L;
                for (int i = 0; i < size; i++) maximum = Math.max(maximum, values[i]);
            } else {
                maximum = Math.max(maximum, value);
            }
        }

        long averageNanos() { return size == 0 ? 0L : sum / size; }
        long maximumNanos() { return maximum; }
        int size() { return size; }
    }

    private record Settings(int surfacePath, int interiorPath, int shallowDepth, int undergroundDepth,
                            int thinCoverage, int undergroundCoverage, int horizontalRadius, int verticalRadius,
                            int nodeLimit, double enclosureThreshold, double undergroundScore) {
        static Settings read() {
            return new Settings(CivitasConfig.REGION_SURFACE_PATH.get(), CivitasConfig.REGION_INTERIOR_PATH.get(),
                    CivitasConfig.REGION_SHALLOW_DEPTH.get(), CivitasConfig.REGION_UNDERGROUND_DEPTH.get(),
                    CivitasConfig.REGION_THIN_COVERAGE.get(), CivitasConfig.REGION_UNDERGROUND_COVERAGE.get(),
                    CivitasConfig.REGION_HORIZONTAL_RADIUS.get(), CivitasConfig.REGION_VERTICAL_RADIUS.get(),
                    CivitasConfig.REGION_NODE_LIMIT.get(), CivitasConfig.REGION_ENCLOSURE_THRESHOLD.get(),
                    CivitasConfig.REGION_UNDERGROUND_SCORE.get());
        }
        RegionClassificationRules.Settings rules() {
            return new RegionClassificationRules.Settings(surfacePath, interiorPath, shallowDepth, undergroundDepth,
                    thinCoverage, undergroundCoverage, enclosureThreshold, undergroundScore);
        }
    }

    private ServerRegionClassifier() {}
}
