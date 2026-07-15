package com.seaquake6324.civitas.domain.civilization;

public enum CivilizationTier {
    WILDERNESS,
    FRONTIER,
    SETTLEMENT,
    SECURE,
    FLOURISHING;

    public static CivilizationTier fromValue(double value) {
        return values()[Math.min(4, Math.max(0, (int)value / 20))];
    }
}
