package com.seaquake6324.civitas.domain.civilization;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/** Bounded, transient evidence for one chunk-layer activity window. */
public record ActivityWindowEvidence(long windowStart, int categoryMask, Set<UUID> contributors,
        boolean contributorOverflow, int directSources, int propagatedSources, double propagatedGain) {
    public static final int MAX_CONTRIBUTORS = 16;

    public ActivityWindowEvidence {
        categoryMask &= 0x1f;
        contributors = contributors == null ? Set.of() : Set.copyOf(contributors);
        if (contributors.size() > MAX_CONTRIBUTORS) throw new IllegalArgumentException("too many contributors");
        directSources = Math.max(0, directSources);
        propagatedSources = Math.max(0, propagatedSources);
        propagatedGain = Math.max(0, propagatedGain);
    }

    public static ActivityWindowEvidence empty(long start) {
        return new ActivityWindowEvidence(start, 0, Set.of(), false, 0, 0, 0);
    }

    public boolean has(ActivityCategory category) { return (categoryMask & category.bit()) != 0; }
    public int categoryCount() { return Integer.bitCount(categoryMask); }
    public int contributorCount() { return contributors.size() + (contributorOverflow ? 1 : 0); }

    public ActivityWindowEvidence record(ActivityCategory category, UUID contributor) {
        int nextMask = categoryMask | category.bit();
        LinkedHashSet<UUID> next = new LinkedHashSet<>(contributors);
        boolean overflow = contributorOverflow;
        if (contributor != null && !next.contains(contributor)) {
            if (next.size() < MAX_CONTRIBUTORS) next.add(contributor); else overflow = true;
        }
        return new ActivityWindowEvidence(windowStart, nextMask, next, overflow,
                directSources + (has(category) ? 0 : 1), propagatedSources, propagatedGain);
    }

    public ActivityWindowEvidence propagate(double gain) {
        return new ActivityWindowEvidence(windowStart, categoryMask, contributors, contributorOverflow,
                directSources, propagatedSources + 1, propagatedGain + Math.max(0, gain));
    }
}
