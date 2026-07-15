package com.seaquake6324.civitas.domain.border;

/** Bounded edge-scan evidence retained for diagnostics; no hostile pathfinding is performed. */
public record BorderFortificationEvidence(int scannedColumns,int continuousWallColumns,int controlledEntrances,
        int unprotectedGaps,int passableGates,int safeInsidePathColumns,double score) {
    public BorderFortificationEvidence {
        if(scannedColumns<0||continuousWallColumns<0||controlledEntrances<0||unprotectedGaps<0||passableGates<0||safeInsidePathColumns<0)throw new IllegalArgumentException();
        score=Math.max(0,Math.min(100,score));
    }
    public static BorderFortificationEvidence calculate(int scanned,int walls,int entrances,int gaps,int gates,int paths){
        return calculate(scanned,walls,entrances,gaps,gates,paths,new Weights(45,15,15,25,50));
    }
    public static BorderFortificationEvidence calculate(int scanned,int walls,int entrances,int gaps,int gates,int paths,Weights weights){
        if(scanned<=0)return new BorderFortificationEvidence(0,0,0,0,0,0,0);
        double wall=walls/(double)scanned*weights.wall,entry=Math.min(1,entrances/2.0)*weights.entrance,gate=Math.min(1,gates/2.0)*weights.gate,path=paths/(double)scanned*weights.path,penalty=gaps/(double)scanned*weights.gapPenalty;
        return new BorderFortificationEvidence(scanned,walls,entrances,gaps,gates,paths,Math.max(0,wall+entry+gate+path-penalty));
    }
    public record Weights(double wall,double entrance,double gate,double path,double gapPenalty){}
}
