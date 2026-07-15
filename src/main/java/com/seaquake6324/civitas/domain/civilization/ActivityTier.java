package com.seaquake6324.civitas.domain.civilization;

public enum ActivityTier {
    DESERTED,
    OCCASIONAL,
    STIRRING,
    BUSY,
    THRIVING;

    public static ActivityTier fromValue(double value) {
        return values()[Math.min(4, Math.max(0, (int)value / 20))];
    }
}
