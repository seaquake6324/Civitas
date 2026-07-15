package com.seaquake6324.civitas.domain.security;
import com.seaquake6324.civitas.domain.territory.NeglectStage;import com.seaquake6324.civitas.domain.territory.TerritoryChunkState;
/** Immutable main-thread snapshot; contains no Minecraft world objects. */
public record SecurityCellEvidence(double civility,double activity,double builtSafety,int borderDistance,boolean borderTopologyLimited,
        NeglectStage neglectStage,TerritoryChunkState.DefenseResult lastDefenseResult,long lastDefenseAt,
        int territorySize,int validBuildings,int validatedEntrances,boolean buildingEvidenceLinked,
        double patrolCoverage,double guardResponse,boolean patrolEvidenceLinked,boolean guardEvidenceLinked,
        double infiltrationAccess,double undergroundDarkness,boolean infiltrationEvidenceLinked,boolean undergroundEvidenceLinked,long assessedAt){
 public SecurityCellEvidence{civility=clamp(civility);activity=clamp(activity);builtSafety=clamp(builtSafety);patrolCoverage=clamp(patrolCoverage);guardResponse=clamp(guardResponse);infiltrationAccess=clamp(infiltrationAccess);undergroundDarkness=clamp(undergroundDarkness);borderDistance=Math.max(0,borderDistance);territorySize=Math.max(1,territorySize);validBuildings=Math.max(0,validBuildings);validatedEntrances=Math.max(0,validatedEntrances);assessedAt=Math.max(0,assessedAt);}
 public SecurityCellEvidence(double civility,double activity,double builtSafety,int borderDistance,boolean borderTopologyLimited,NeglectStage neglectStage,TerritoryChunkState.DefenseResult lastDefenseResult,long lastDefenseAt,int territorySize,int validBuildings,int validatedEntrances,boolean buildingEvidenceLinked,long assessedAt){this(civility,activity,builtSafety,borderDistance,borderTopologyLimited,neglectStage,lastDefenseResult,lastDefenseAt,territorySize,validBuildings,validatedEntrances,buildingEvidenceLinked,0,0,false,false,0,0,false,false,assessedAt);}
 private static double clamp(double value){return Math.max(0,Math.min(100,value));}
 public SecurityCellEvidence(double civility,double activity,double builtSafety,int borderDistance,boolean borderTopologyLimited,
        NeglectStage neglectStage,TerritoryChunkState.DefenseResult lastDefenseResult,long lastDefenseAt,
        int territorySize,int validBuildings,int validatedEntrances,long assessedAt){this(civility,activity,builtSafety,borderDistance,borderTopologyLimited,neglectStage,lastDefenseResult,lastDefenseAt,territorySize,validBuildings,validatedEntrances,false,0,0,false,false,0,0,false,false,assessedAt);}
 public SecurityCellEvidence(double civility,double activity,double builtSafety,int borderDistance,boolean borderTopologyLimited,
        NeglectStage neglectStage,TerritoryChunkState.DefenseResult lastDefenseResult,long lastDefenseAt,
        int territorySize,int validBuildings,int validatedEntrances,boolean buildingEvidenceLinked,
        double patrolCoverage,double guardResponse,boolean patrolEvidenceLinked,boolean guardEvidenceLinked,long assessedAt){this(civility,activity,builtSafety,borderDistance,borderTopologyLimited,neglectStage,lastDefenseResult,lastDefenseAt,territorySize,validBuildings,validatedEntrances,buildingEvidenceLinked,patrolCoverage,guardResponse,patrolEvidenceLinked,guardEvidenceLinked,0,0,false,false,assessedAt);}
}
