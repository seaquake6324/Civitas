package com.seaquake6324.civitas.domain.security;

/** Minecraft-free candidate and complete weight explanation for one legal entry point. */
public record InfiltrationCandidate(InfiltrationSource source, long position, long chunk,
        double sourceWeight, double securityRisk, double darkness, double unguarded,
        double finalWeight) {
    public InfiltrationCandidate {
        if (source == null || sourceWeight < 0 || finalWeight < 0) throw new IllegalArgumentException("invalid infiltration candidate");
        securityRisk = clamp(securityRisk); darkness = clamp(darkness); unguarded = clamp(unguarded);
    }
    private static double clamp(double value) { return Math.max(0, Math.min(100, value)); }
}
