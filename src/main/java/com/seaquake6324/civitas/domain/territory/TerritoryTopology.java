package com.seaquake6324.civitas.domain.territory;

import com.seaquake6324.civitas.domain.ChunkCoordinate;
import java.util.ArrayDeque;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;

public final class TerritoryTopology {
    public enum Direction { NORTH(0,-1), EAST(1,0), SOUTH(0,1), WEST(-1,0); final int dx,dz; Direction(int dx,int dz){this.dx=dx;this.dz=dz;} }

    public static long adjacent(long chunk, Direction direction) {
        ChunkCoordinate c = ChunkCoordinate.unpack(chunk);
        return ChunkCoordinate.pack(c.x() + direction.dx, c.z() + direction.dz);
    }
    public static EnumSet<Direction> borderEdges(Set<Long> territory, long chunk) {
        EnumSet<Direction> result = EnumSet.noneOf(Direction.class);
        if (!territory.contains(chunk)) return result;
        for (Direction d : Direction.values()) if (!territory.contains(adjacent(chunk, d))) result.add(d);
        return result;
    }
    public static boolean touches(Set<Long> territory, long chunk) {
        for (Direction d : Direction.values()) if (territory.contains(adjacent(chunk, d))) return true;
        return false;
    }
    public static boolean connected(Set<Long> territory) {
        if (territory.isEmpty()) return true;
        Set<Long> visited = new HashSet<>();
        ArrayDeque<Long> queue = new ArrayDeque<>();
        queue.add(territory.iterator().next());
        while (!queue.isEmpty()) {
            long at = queue.removeFirst();
            if (!visited.add(at)) continue;
            for (Direction d : Direction.values()) { long next = adjacent(at,d); if (territory.contains(next) && !visited.contains(next)) queue.add(next); }
        }
        return visited.size() == territory.size();
    }
    public static boolean removable(Set<Long> territory, long chunk, long coreChunk) {
        if (chunk == coreChunk || !territory.contains(chunk)) return false;
        Set<Long> remainder = new HashSet<>(territory); remainder.remove(chunk);
        return remainder.contains(coreChunk) && connected(remainder);
    }
    private TerritoryTopology() {}
}
