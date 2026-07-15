package com.seaquake6324.civitas.domain.civilization;

public record CivilityCandidate(String fingerprint, CivilityEvidence evidence,
        CivilityScore score, long firstSeen, long scannedAt) {
    public CivilityCandidate {
        fingerprint = fingerprint == null ? "" : fingerprint;
        evidence = evidence == null ? CivilityEvidence.empty() : evidence;
        score = score == null ? CivilityScoringRules.score(evidence) : score;
    }
}
