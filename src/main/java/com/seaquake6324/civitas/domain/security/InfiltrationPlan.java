package com.seaquake6324.civitas.domain.security;

import java.util.List;
import java.util.UUID;

/** Persisted event-site decision; keeps every bounded candidate and its weight, not only the winner. */
public record InfiltrationPlan(UUID cityId, long cityRevision, List<InfiltrationCandidate> candidates,
        int selectedIndex, long assessedAt, int examinedCells, int worldSamples, boolean truncated) {
    public static final int HARD_CANDIDATE_CAP = 512;
    public InfiltrationPlan {
        if (cityId == null || cityRevision < 0 || candidates == null || candidates.isEmpty()
                || candidates.size() > HARD_CANDIDATE_CAP || selectedIndex < 0 || selectedIndex >= candidates.size()
                || assessedAt < 0 || examinedCells < 0 || worldSamples < 0) throw new IllegalArgumentException("invalid infiltration plan");
        candidates = List.copyOf(candidates);
    }
    public InfiltrationCandidate selected() { return candidates.get(selectedIndex); }
}
