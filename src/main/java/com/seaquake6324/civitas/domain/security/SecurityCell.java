package com.seaquake6324.civitas.domain.security;
import java.util.Map;import java.util.Set;import java.util.UUID;
/** Persisted risk explanation consumed by the legal infiltration selector. */
public record SecurityCell(UUID cityId,long chunk,SecurityCellEvidence evidence,Map<SecurityFactor,SecurityContribution>contributions,
        double diagnosticRisk,SecurityFactor primaryFactor,Set<MissingEvidence>missingEvidence,long revision){
 public SecurityCell{contributions=Map.copyOf(contributions);missingEvidence=Set.copyOf(missingEvidence);diagnosticRisk=Math.max(0,Math.min(100,diagnosticRisk));revision=Math.max(0,revision);}
 public enum MissingEvidence{PATROL_COVERAGE,GUARD_RESPONSE,INFILTRATION_SOURCE,UNDERGROUND_DARKNESS,BUILDING_RECORD_LINK,BORDER_TOPOLOGY_LIMIT}
}
