package com.seaquake6324.civitas.domain.population;

import java.util.LinkedHashMap;
import java.util.Map;

/** Pure scoring for a real orphan and a spatially evidenced local family. */
public final class AdoptionRules {
    public Result evaluate(Candidate candidate,double distanceBlocks,double maximumDistance){
        return evaluate(candidate,distanceBlocks,maximumDistance,false);
    }
    public Result evaluate(Candidate candidate,double distanceBlocks,double maximumDistance,boolean playerConsentComplete){
        if(candidate==null||!Double.isFinite(distanceBlocks)||!Double.isFinite(maximumDistance)||maximumDistance<=0)throw new IllegalArgumentException("invalid adoption input");
        boolean eligible=candidate.guardianCount()>0&&(!candidate.includesPlayer()||playerConsentComplete)&&candidate.remainingCapacity()>0&&distanceBlocks<=maximumDistance;
        LinkedHashMap<String,Double> parts=new LinkedHashMap<>();
        add(parts,"distance",100*(1-distanceBlocks/maximumDistance),.30);add(parts,"housing",candidate.housing(),.20);add(parts,"food",candidate.food(),.15);add(parts,"safety",candidate.safety(),.15);add(parts,"willingness",candidate.willingness(),.15);add(parts,"capacity",100.0*candidate.remainingCapacity()/candidate.maximumChildren(),.05);
        double score=eligible?parts.values().stream().mapToDouble(Double::doubleValue).sum():0;
        return new Result(eligible,Math.max(0,Math.min(100,score)),distanceBlocks,Map.copyOf(parts),eligible?"eligible":candidate.includesPlayer()&&!playerConsentComplete?"player_consent_required":candidate.remainingCapacity()<=0?"capacity":"distance_or_guardian");
    }
    private static void add(Map<String,Double>parts,String key,double value,double weight){parts.put(key,Math.max(0,Math.min(100,value))*weight);}
    public record Candidate(int guardianCount,boolean includesPlayer,int remainingCapacity,int maximumChildren,double housing,double food,double safety,double willingness){public Candidate{if(guardianCount<0||remainingCapacity<0||maximumChildren<1)throw new IllegalArgumentException("invalid adoption candidate");}}
    public record Result(boolean eligible,double score,double distanceBlocks,Map<String,Double>components,String reason){}
}
