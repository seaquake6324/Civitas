package com.seaquake6324.civitas.domain.security;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Central first-pass weighting and deterministic weighted selection. */
public final class InfiltrationSelectionRules {
    public InfiltrationPlan select(UUID cityId, long cityRevision, List<Evidence> evidence, Weights weights,
            double draw, long assessedAt, int examinedCells, int worldSamples, boolean truncated, int candidateCap) {
        if (evidence == null || evidence.isEmpty()) throw new IllegalArgumentException("no infiltration candidates");
        int cap = Math.max(1, Math.min(InfiltrationPlan.HARD_CANDIDATE_CAP, candidateCap));
        ArrayList<InfiltrationCandidate> candidates = new ArrayList<>(Math.min(cap, evidence.size()));
        evidence.stream().sorted(Comparator.comparingLong(Evidence::chunk).thenComparingLong(Evidence::position)
                .thenComparing(e -> e.source().ordinal())).limit(cap).forEach(e -> {
            double source = weights.source(e.source());
            double unguarded = 100 - Math.max(e.patrolCoverage(), e.guardResponse());
            double factor = weighted(e.securityRisk(), e.darkness(), unguarded, weights);
            candidates.add(new InfiltrationCandidate(e.source(), e.position(), e.chunk(), source,
                    e.securityRisk(), e.darkness(), unguarded, source * Math.max(.01, factor / 100.0)));
        });
        double total = candidates.stream().mapToDouble(InfiltrationCandidate::finalWeight).sum();
        double cursor = Math.max(0, Math.min(Math.nextDown(1.0), draw)) * total;
        int selected = candidates.size() - 1;
        for (int i = 0; i < candidates.size(); i++) { cursor -= candidates.get(i).finalWeight(); if (cursor < 0) { selected = i; break; } }
        return new InfiltrationPlan(cityId, cityRevision, candidates, selected, assessedAt, examinedCells,
                worldSamples, truncated || evidence.size() > cap);
    }

    private static double weighted(double security, double darkness, double unguarded, Weights w) {
        double total = w.securityRisk() + w.darkness() + w.unguarded();
        return total <= 0 ? 100 : (security * w.securityRisk() + darkness * w.darkness() + unguarded * w.unguarded()) / total;
    }

    public record Evidence(InfiltrationSource source, long position, long chunk, double securityRisk,
            double darkness, double patrolCoverage, double guardResponse) {
        public Evidence { if (source == null) throw new IllegalArgumentException("missing infiltration source"); }
    }

    public record Weights(double uncontrolledEntrance, double wallGap, double cave, double undergroundDarkness,
            double disguisedEntry, double securityRisk, double darkness, double unguarded) {
        public Weights {
            if (uncontrolledEntrance < 0 || wallGap < 0 || cave < 0 || undergroundDarkness < 0
                    || disguisedEntry < 0 || securityRisk < 0 || darkness < 0 || unguarded < 0)
                throw new IllegalArgumentException("invalid infiltration weights");
        }
        public double source(InfiltrationSource source) { return switch (source) {
            case UNCONTROLLED_ENTRANCE -> uncontrolledEntrance;
            case WALL_GAP -> wallGap;
            case CAVE -> cave;
            case UNDERGROUND_DARKNESS -> undergroundDarkness;
            case DISGUISED_ENTRY -> disguisedEntry;
        }; }
    }
}
