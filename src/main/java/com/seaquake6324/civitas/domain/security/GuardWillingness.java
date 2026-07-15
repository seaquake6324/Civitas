package com.seaquake6324.civitas.domain.security;

import java.util.Map;

/** Explainable appointment willingness; forced appointments preserve the rejected score. */
public record GuardWillingness(double score, Map<String, Double> components) {
    public GuardWillingness {
        score = clamp(score);
        components = Map.copyOf(components);
    }

    private static double clamp(double value) { return Math.max(0, Math.min(100, value)); }
}
