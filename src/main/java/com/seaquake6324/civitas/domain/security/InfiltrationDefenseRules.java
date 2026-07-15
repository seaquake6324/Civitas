package com.seaquake6324.civitas.domain.security;

/** First-pass security effects: detection may intercept, otherwise extends warning and reduces the wave. */
public final class InfiltrationDefenseRules {
    public Result evaluate(double localRisk, double draw, int maximumWarningBonusTicks, double minimumWaveScale) {
        double risk = Math.max(0, Math.min(100, localRisk));
        double normalized = risk / 100.0;
        boolean intercepted = Math.max(0, Math.min(Math.nextDown(1.0), draw)) >= normalized;
        int warningBonus = (int)Math.round(Math.max(0, maximumWarningBonusTicks) * (1 - normalized));
        double scale = Math.max(0, Math.min(1, minimumWaveScale)) + (1 - Math.max(0, Math.min(1, minimumWaveScale))) * normalized;
        return new Result(intercepted, warningBonus, scale, risk);
    }
    public record Result(boolean intercepted, int warningBonusTicks, double waveScale, double localRisk) {}
}
