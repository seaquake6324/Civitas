package com.seaquake6324.civitas.domain.civilization;

/** Scores produced by scanners; this domain type deliberately has no Minecraft dependency. */
public record CivilizationFactors(double building, double facilities, double safety, double connectivity) {
    public CivilizationFactors {
        building = clamp(building);
        facilities = clamp(facilities);
        safety = clamp(safety);
        connectivity = clamp(connectivity);
    }

    public static CivilizationFactors empty() { return new CivilizationFactors(0, 0, 0, 0); }

    public double target() {
        double target = building * 0.35 + facilities * 0.25 + safety * 0.25 + connectivity * 0.15;
        if (building < 20 || connectivity < 20) target = Math.min(target, 39);
        if (facilities < 20 || safety < 20) target = Math.min(target, 59);
        return clamp(target);
    }

    private static double clamp(double value) { return Math.max(0, Math.min(100, value)); }
}
