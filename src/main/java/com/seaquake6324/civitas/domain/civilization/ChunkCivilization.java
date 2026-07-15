package com.seaquake6324.civitas.domain.civilization;

/** Persisted state for exactly one vertical layer in one X/Z chunk. */
public record ChunkCivilization(CivilizationFactors factors, double targetCivility, double civility,
        double activity, long stableSince, long lastEvaluated, CivilityEvidence evidence,
        CivilityScore score, CivilityCandidate candidate, BoundaryPorts boundaryPorts, long lastScanned,
        ActivitySummary activitySummary) {
    public ChunkCivilization {
        factors = factors == null ? CivilizationFactors.empty() : factors;
        targetCivility = clamp(targetCivility);
        civility = clamp(civility);
        activity = clamp(activity);
        evidence = evidence == null ? CivilityEvidence.empty() : evidence;
        score = score == null ? new CivilityScore(factors, targetCivility, targetCivility, java.util.List.of()) : score;
        boundaryPorts = boundaryPorts == null ? BoundaryPorts.empty() : boundaryPorts;
        activitySummary = activitySummary == null ? ActivitySummary.empty() : activitySummary;
    }

    public ChunkCivilization(CivilizationFactors factors, double targetCivility, double civility,
            double activity, long stableSince, long lastEvaluated) {
        this(factors, targetCivility, civility, activity, stableSince, lastEvaluated,
                CivilityEvidence.empty(), null, null, BoundaryPorts.empty(), 0, ActivitySummary.empty());
    }

    public ChunkCivilization(CivilizationFactors factors, double targetCivility, double civility,
            double activity, long stableSince, long lastEvaluated, CivilityEvidence evidence,
            CivilityScore score, CivilityCandidate candidate, BoundaryPorts boundaryPorts, long lastScanned) {
        this(factors, targetCivility, civility, activity, stableSince, lastEvaluated, evidence,
                score, candidate, boundaryPorts, lastScanned, ActivitySummary.empty());
    }

    public static ChunkCivilization empty() {
        return new ChunkCivilization(CivilizationFactors.empty(), 0, 0, 0, 0, 0);
    }

    public ChunkCivilization withFactors(CivilizationFactors next, long gameTime) {
        return new ChunkCivilization(next, next.target(), civility, activity, gameTime, gameTime,
                evidence, new CivilityScore(next, next.target(), next.target(), java.util.List.of()),
                candidate, boundaryPorts, lastScanned, activitySummary);
    }

    public ChunkCivilization approachTarget(double growth, double decline, long gameTime) {
        double rate = targetCivility >= civility ? Math.max(0, growth) : Math.max(0, decline);
        double next = civility + Math.copySign(Math.min(Math.abs(targetCivility - civility), rate), targetCivility - civility);
        return new ChunkCivilization(factors, targetCivility, next, activity, stableSince, gameTime,
                evidence, score, candidate, boundaryPorts, lastScanned, activitySummary);
    }

    public ChunkCivilization withActivity(double next, long gameTime) {
        return new ChunkCivilization(factors, targetCivility, civility, next, stableSince, gameTime,
                evidence, score, candidate, boundaryPorts, lastScanned, activitySummary);
    }

    public ChunkCivilization withActivity(double next, ActivitySummary summary) {
        return new ChunkCivilization(factors, targetCivility, civility, next, stableSince, lastEvaluated,
                evidence, score, candidate, boundaryPorts, lastScanned, summary);
    }

    public CivilizationTier civilityTier() { return CivilizationTier.fromValue(civility); }
    public ActivityTier activityTier() { return ActivityTier.fromValue(activity); }

    private static double clamp(double value) { return Math.max(0, Math.min(100, value)); }
}
