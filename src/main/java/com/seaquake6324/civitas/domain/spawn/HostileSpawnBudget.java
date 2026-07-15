package com.seaquake6324.civitas.domain.spawn;

/** Pure rules for the shared natural-hostile cap and its two classified pools. */
public final class HostileSpawnBudget {
    public static final int SURFACE_POOL = 0;
    public static final int UNDERGROUND_POOL = 1;
    public static final double MAX_POOL_SHARE = 0.60;

    public enum Decision {
        ALLOW,
        TOTAL_CAP,
        POOL_CAP,
        CIVILITY
    }

    public record Limits(int totalCap, int poolCap) {
        public Limits {
            if (totalCap < 0) throw new IllegalArgumentException("Total cap cannot be negative");
            if (poolCap < 0 || poolCap > totalCap) {
                throw new IllegalArgumentException("Pool cap must be between zero and the total cap");
            }
        }
    }

    public static Limits limits(int perChunk, int spawnableChunks, int spawnArea,
            double multiplier, double poolShare) {
        if (perChunk < 0 || spawnableChunks < 0) {
            throw new IllegalArgumentException("Spawn counts cannot be negative");
        }
        if (spawnArea <= 0) throw new IllegalArgumentException("Spawn area must be positive");
        if (!Double.isFinite(multiplier) || multiplier < 0.0) {
            throw new IllegalArgumentException("Multiplier must be finite and non-negative");
        }
        if (!Double.isFinite(poolShare) || poolShare < 0.0 || poolShare > 1.0) {
            throw new IllegalArgumentException("Pool share must be between zero and one");
        }

        double scaled = perChunk * (double)spawnableChunks / spawnArea * multiplier;
        int totalCap = scaled >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)Math.floor(scaled);
        int poolCap = (int)Math.floor(totalCap * poolShare);
        return new Limits(totalCap, poolCap);
    }

    /**
     * Returns exactly one outcome. Capacity failures take precedence over randomized civility suppression so
     * diagnostics can attribute every rejection consistently.
     */
    public static Decision evaluate(Limits limits, int surfaceCount, int undergroundCount, int poolIndex,
            double suppression, double roll) {
        if (limits == null) throw new IllegalArgumentException("Limits are required");
        if (surfaceCount < 0 || undergroundCount < 0) {
            throw new IllegalArgumentException("Pool counts cannot be negative");
        }
        if (poolIndex != SURFACE_POOL && poolIndex != UNDERGROUND_POOL) {
            throw new IllegalArgumentException("Unknown hostile pool: " + poolIndex);
        }
        if (!Double.isFinite(suppression) || suppression < 0.0 || suppression > 1.0) {
            throw new IllegalArgumentException("Suppression must be between zero and one");
        }
        if (!Double.isFinite(roll) || roll < 0.0 || roll >= 1.0) {
            throw new IllegalArgumentException("Roll must be in [0, 1)");
        }

        long total = (long)surfaceCount + undergroundCount;
        if (total >= limits.totalCap()) return Decision.TOTAL_CAP;
        int poolCount = poolIndex == SURFACE_POOL ? surfaceCount : undergroundCount;
        if (poolCount >= limits.poolCap()) return Decision.POOL_CAP;
        return roll < suppression ? Decision.CIVILITY : Decision.ALLOW;
    }

    private HostileSpawnBudget() {}
}
