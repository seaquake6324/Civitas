package com.seaquake6324.civitas.domain;

/** A Minecraft-independent chunk coordinate and its stable packed representation. */
public record ChunkCoordinate(int x, int z) {
    public long packed() { return pack(x, z); }

    public long chebyshevDistance(ChunkCoordinate other) {
        return Math.max(Math.abs((long)x - other.x), Math.abs((long)z - other.z));
    }

    public static long pack(int x, int z) {
        return (x & 0xFFFFFFFFL) | ((z & 0xFFFFFFFFL) << 32);
    }

    public static ChunkCoordinate unpack(long packed) {
        return new ChunkCoordinate((int)packed, (int)(packed >>> 32));
    }
}
