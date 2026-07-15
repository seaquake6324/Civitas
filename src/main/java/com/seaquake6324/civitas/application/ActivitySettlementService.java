package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.domain.civilization.ActivityRules;
import com.seaquake6324.civitas.domain.civilization.ActivitySummary;
import com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence;
import com.seaquake6324.civitas.domain.civilization.ChunkCivilization;

/** Application use case that settles bounded window evidence into persisted chunk state. */
public final class ActivitySettlementService {
    public Result settle(ChunkCivilization state, ActivityWindowEvidence evidence, long now, Settings settings) {
        double direct = ActivityRules.directGain(evidence.categoryCount(), evidence.contributorCount(),
                settings.gainPerCategory(), settings.localGainCap());
        double propagated = Math.min(settings.propagationCap(), evidence.propagatedGain());
        double total = ActivityRules.combinedGain(direct, propagated, settings.localGainCap());
        ActivitySummary summary = state.activitySummary().settled(now, evidence.categoryMask(), direct,
                propagated, evidence.contributorCount());
        ChunkCivilization updated = state.withActivity(ActivityRules.clamp(state.activity() + total), summary);
        return new Result(updated, direct, ActivityRules.propagatedGain(direct, settings.propagationRatio()));
    }

    public ChunkCivilization decay(ChunkCivilization state, long now, Settings settings) {
        ActivityRules.DecayResult result = ActivityRules.decay(state.activity(), state.activitySummary(), now,
                settings.graceTicks(), settings.windowTicks(), settings.decayPerWindow());
        if (result.periods() == 0) return state;
        return state.withActivity(result.activity(), state.activitySummary().decayed(result.evaluatedAt(), result.periods()));
    }

    public record Settings(long windowTicks, long graceTicks, double decayPerWindow, double gainPerCategory,
            double propagationRatio, double propagationCap, double localGainCap) {}
    public record Result(ChunkCivilization state, double directGain, double outwardPropagationGain) {}
}
