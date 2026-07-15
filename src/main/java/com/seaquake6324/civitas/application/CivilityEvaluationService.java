package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.domain.civilization.BoundaryPorts;
import com.seaquake6324.civitas.domain.civilization.ChunkCivilization;
import com.seaquake6324.civitas.domain.civilization.CivilityCandidate;
import com.seaquake6324.civitas.domain.civilization.CivilityEvidence;
import com.seaquake6324.civitas.domain.civilization.CivilityScore;
import com.seaquake6324.civitas.domain.civilization.CivilityScoringRules;

/** Single use case for candidate comparison, stable commit and elapsed-cycle evolution. */
public final class CivilityEvaluationService {
    public static final long EVOLUTION_CYCLE_TICKS = 200;

    public ChunkCivilization evaluate(ChunkCivilization state, ScanSnapshot scan, long now,
            long stabilityTicks, double growth, double decline) {
        return evaluate(state,scan,now,stabilityTicks,growth,decline,new CivilityScoringRules.Weights(.35,.25,.25,.15));
    }
    public ChunkCivilization evaluate(ChunkCivilization state, ScanSnapshot scan, long now,
            long stabilityTicks, double growth, double decline,CivilityScoringRules.Weights weights) {
        state = evolve(state, now, growth, decline);
        CivilityScore score = CivilityScoringRules.score(scan.evidence(),weights);
        CivilityCandidate previous = state.candidate();
        long firstSeen = previous != null && previous.fingerprint().equals(scan.fingerprint())
                ? previous.firstSeen() : now;
        CivilityCandidate candidate = new CivilityCandidate(scan.fingerprint(), scan.evidence(), score, firstSeen, now);
        if (now - firstSeen < stabilityTicks) {
            return copy(state, state.factors(), state.targetCivility(), state.evidence(), state.score(),
                    candidate, scan.evidence().ports(), now, firstSeen);
        }
        return copy(state, score.factors(), score.target(), scan.evidence(), score,
                null, scan.evidence().ports(), now, 0);
    }

    public ChunkCivilization evolve(ChunkCivilization state, long now, double growth, double decline) {
        if (state.lastEvaluated() <= 0 || now <= state.lastEvaluated()) return copy(state, state.factors(),
                state.targetCivility(), state.evidence(), state.score(), state.candidate(), state.boundaryPorts(),
                state.lastScanned(), state.stableSince(), now);
        long cycles = (now - state.lastEvaluated()) / EVOLUTION_CYCLE_TICKS;
        if (cycles <= 0) return state;
        double delta = state.targetCivility() >= state.civility() ? growth * cycles : -decline * cycles;
        double next = delta >= 0 ? Math.min(state.targetCivility(), state.civility() + delta)
                : Math.max(state.targetCivility(), state.civility() + delta);
        long evaluated = state.lastEvaluated() + cycles * EVOLUTION_CYCLE_TICKS;
        return new ChunkCivilization(state.factors(), state.targetCivility(), next, state.activity(),
                state.stableSince(), evaluated, state.evidence(), state.score(), state.candidate(),
                state.boundaryPorts(), state.lastScanned(), state.activitySummary());
    }

    private static ChunkCivilization copy(ChunkCivilization state,
            com.seaquake6324.civitas.domain.civilization.CivilizationFactors factors, double target,
            CivilityEvidence evidence, CivilityScore score, CivilityCandidate candidate,
            BoundaryPorts ports, long lastScanned, long stableSince) {
        return copy(state, factors, target, evidence, score, candidate, ports, lastScanned, stableSince, state.lastEvaluated());
    }

    private static ChunkCivilization copy(ChunkCivilization state,
            com.seaquake6324.civitas.domain.civilization.CivilizationFactors factors, double target,
            CivilityEvidence evidence, CivilityScore score, CivilityCandidate candidate,
            BoundaryPorts ports, long lastScanned, long stableSince, long lastEvaluated) {
        return new ChunkCivilization(factors, target, state.civility(), state.activity(), stableSince,
                lastEvaluated, evidence, score, candidate, ports, lastScanned, state.activitySummary());
    }

    public record ScanSnapshot(String fingerprint, CivilityEvidence evidence) {
        public ScanSnapshot { fingerprint = fingerprint == null ? "" : fingerprint; }
    }
}
