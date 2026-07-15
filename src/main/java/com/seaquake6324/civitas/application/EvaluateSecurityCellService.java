package com.seaquake6324.civitas.application;
import com.seaquake6324.civitas.domain.security.*;import java.util.UUID;
/** Application use case for a diagnostic SecurityCell computed from an immutable snapshot. */
public final class EvaluateSecurityCellService{private final SecurityRiskRules rules=new SecurityRiskRules();public SecurityCell evaluate(UUID cityId,long chunk,SecurityCellEvidence evidence,SecurityRiskRules.Weights weights,long previousRevision){return rules.evaluate(cityId,chunk,evidence,weights,Math.max(1,previousRevision+1));}}
