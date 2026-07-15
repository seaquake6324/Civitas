package com.seaquake6324.civitas.domain.civilization;

/** Pure activity gain, propagation, clamp, grace, and decay rules. */
public final class ActivityRules {
    public static double directGain(int categories, int contributors, double gainPerCategory, double cap) {
        int c = Math.max(0, Math.min(5, categories));
        if (c == 0) return 0;
        int p = Math.max(1, Math.min(4, contributors));
        double diversity = (c >= 3 ? .5 : 0) + (c == 5 ? .5 : 0);
        double multiplayer = 1 + .2 * Math.min(3, p - 1);
        return Math.min(Math.max(0, cap), (Math.max(0, gainPerCategory) * c + diversity) * multiplayer);
    }

    public static double propagatedGain(double directGain, double ratio) {
        return Math.max(0, directGain) * Math.max(0, ratio);
    }

    public static double capPropagated(double accumulated, double addition, double propagationCap) {
        return Math.min(Math.max(0, propagationCap), Math.max(0, accumulated) + Math.max(0, addition));
    }

    public static double combinedGain(double direct, double propagated, double localCap) {
        return Math.min(Math.max(0, localCap), Math.max(0, direct) + Math.max(0, propagated));
    }

    public static double clamp(double value) { return Math.max(0, Math.min(100, value)); }

    public static DecayResult decay(double activity, ActivitySummary summary, long now, long graceTicks,
            long windowTicks, double decayPerWindow) {
        long anchor = summary.lastActivityTime();
        long eligibleAt = anchor + Math.max(0, graceTicks);
        long evaluated = Math.max(summary.lastEvaluated(), eligibleAt);
        if (anchor <= 0 || now <= evaluated || windowTicks <= 0) return new DecayResult(clamp(activity), 0, summary.lastEvaluated());
        int periods = (int)Math.min(Integer.MAX_VALUE, (now - evaluated) / windowTicks);
        if (periods <= 0) return new DecayResult(clamp(activity), 0, summary.lastEvaluated());
        return new DecayResult(clamp(activity - periods * Math.max(0, decayPerWindow)), periods,
                evaluated + periods * windowTicks);
    }

    public record DecayResult(double activity, int periods, long evaluatedAt) {}
    private ActivityRules() {}
}
