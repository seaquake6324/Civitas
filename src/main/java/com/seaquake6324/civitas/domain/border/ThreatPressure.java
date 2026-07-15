package com.seaquake6324.civitas.domain.border;
public record ThreatPressure(double value, long lastUpdatedAt, long defenseBufferUntil, int failedDefenses) {
    public record Breakdown(double readinessGain,double citySizeGain,double defenseReduction,double finalGain){}
    public Update accumulate(BorderReadiness readiness,int territoryChunks,long now,double basePerTick,double sizeFactor){
        double readinessGain=basePerTick*(1-readiness.total()/100.0); double sizeGain=basePerTick*Math.log1p(Math.max(0,territoryChunks))*sizeFactor;
        double buffer=now<defenseBufferUntil?readinessGain+sizeGain:0; double gain=Math.max(0,readinessGain+sizeGain-buffer);
        return new Update(new ThreatPressure(Math.min(100,value+gain),now,defenseBufferUntil,failedDefenses),new Breakdown(readinessGain,sizeGain,buffer,gain));
    }
    public record Update(ThreatPressure pressure,Breakdown breakdown){}
}
