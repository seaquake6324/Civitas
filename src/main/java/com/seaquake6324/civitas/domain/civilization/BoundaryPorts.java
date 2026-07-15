package com.seaquake6324.civitas.domain.civilization;

/** Four 16-bit edge masks. A matching bit means a passable cross-chunk port. */
public record BoundaryPorts(int north, int east, int south, int west) {
    public BoundaryPorts {
        north &= 0xffff; east &= 0xffff; south &= 0xffff; west &= 0xffff;
    }

    public static BoundaryPorts empty() { return new BoundaryPorts(0, 0, 0, 0); }
    public boolean any() { return (north | east | south | west) != 0; }
    public boolean connects(Direction direction, BoundaryPorts neighbor) {
        if (neighbor == null) return false;
        return (mask(direction) & neighbor.mask(direction.opposite())) != 0;
    }
    public int mask(Direction direction) {
        return switch (direction) { case NORTH -> north; case EAST -> east; case SOUTH -> south; case WEST -> west; };
    }
    public enum Direction {
        NORTH, EAST, SOUTH, WEST;
        public Direction opposite() { return values()[(ordinal() + 2) & 3]; }
    }
}
