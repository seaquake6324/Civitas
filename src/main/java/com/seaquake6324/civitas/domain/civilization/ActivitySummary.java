package com.seaquake6324.civitas.domain.civilization;

import java.util.Arrays;

/** Persisted, bounded explanation of recent activity calculations. */
public record ActivitySummary(long lastActivityTime, long lastEvaluated, long[] categoryLastSeen,
        int lastCategoryMask, double lastDirectGain, double lastPropagatedGain, int lastContributorCount,
        int lastDecayPeriods) {
    public ActivitySummary {
        categoryLastSeen = categoryLastSeen == null ? new long[5] : categoryLastSeen.clone();
        if (categoryLastSeen.length != 5) categoryLastSeen = Arrays.copyOf(categoryLastSeen, 5);
        lastCategoryMask &= 0x1f;
        lastDirectGain = Math.max(0, lastDirectGain);
        lastPropagatedGain = Math.max(0, lastPropagatedGain);
        lastContributorCount = Math.max(0, lastContributorCount);
        lastDecayPeriods = Math.max(0, lastDecayPeriods);
    }

    @Override public long[] categoryLastSeen() { return categoryLastSeen.clone(); }
    @Override public boolean equals(Object other) {
        return other instanceof ActivitySummary value && lastActivityTime == value.lastActivityTime
                && lastEvaluated == value.lastEvaluated && Arrays.equals(categoryLastSeen, value.categoryLastSeen)
                && lastCategoryMask == value.lastCategoryMask
                && Double.compare(lastDirectGain, value.lastDirectGain) == 0
                && Double.compare(lastPropagatedGain, value.lastPropagatedGain) == 0
                && lastContributorCount == value.lastContributorCount && lastDecayPeriods == value.lastDecayPeriods;
    }
    @Override public int hashCode() {
        return 31 * java.util.Objects.hash(lastActivityTime, lastEvaluated, lastCategoryMask, lastDirectGain,
                lastPropagatedGain, lastContributorCount, lastDecayPeriods) + Arrays.hashCode(categoryLastSeen);
    }
    public static ActivitySummary empty() { return new ActivitySummary(0, 0, new long[5], 0, 0, 0, 0, 0); }

    public ActivitySummary settled(long now, int mask, double direct, double propagated, int contributors) {
        long[] seen = categoryLastSeen.clone();
        for (ActivityCategory category : ActivityCategory.values()) if ((mask & category.bit()) != 0) seen[category.ordinal()] = now;
        long active = direct > 0 || propagated > 0 ? now : lastActivityTime;
        return new ActivitySummary(active, now, seen, mask, direct, propagated, contributors, 0);
    }

    public ActivitySummary decayed(long evaluatedAt, int periods) {
        return new ActivitySummary(lastActivityTime, evaluatedAt, categoryLastSeen, lastCategoryMask,
                lastDirectGain, lastPropagatedGain, lastContributorCount, periods);
    }
}
