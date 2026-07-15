package com.seaquake6324.civitas.domain.civilization;

import java.util.List;

public record CivilityScore(CivilizationFactors factors, double uncappedTarget,
        double target, List<CivilityLimitReason> limits) {
    public CivilityScore {
        factors = factors == null ? CivilizationFactors.empty() : factors;
        uncappedTarget = clamp(uncappedTarget);
        target = clamp(target);
        limits = limits == null ? List.of() : List.copyOf(limits);
    }
    private static double clamp(double value) { return Math.max(0, Math.min(100, value)); }
}
