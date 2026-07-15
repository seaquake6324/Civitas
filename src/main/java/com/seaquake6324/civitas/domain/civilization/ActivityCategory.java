package com.seaquake6324.civitas.domain.civilization;

/** Bounded activity dimensions; ordinals are persisted in a five-bit mask. */
public enum ActivityCategory {
    CONSTRUCTION,
    PRODUCTION,
    LIVELIHOOD,
    DEFENSE,
    TRANSIT;

    public int bit() { return 1 << ordinal(); }
}
