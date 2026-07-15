package com.seaquake6324.civitas.domain.population;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Explainable first-pass city attraction; no value is a hard settlement gate. */
public final class MigrationAttractionRules {
    public Result evaluate(Inputs inputs) {
        LinkedHashMap<String,Double> components=new LinkedHashMap<>();
        add(components,"housing",inputs.housing(),.30);
        add(components,"food",inputs.food(),.20);
        add(components,"safety",inputs.safety(),.20);
        add(components,"civility",inputs.civility(),.15);
        add(components,"activity",inputs.activity(),.15);
        double score=components.values().stream().mapToDouble(Double::doubleValue).sum();
        return new Result(score,Math.max(.02,score/100.0),Collections.unmodifiableMap(components));
    }
    private static void add(Map<String,Double> result,String key,double value,double weight){result.put(key,clamp(value)*weight);}
    private static double clamp(double value){if(!Double.isFinite(value))throw new IllegalArgumentException("non-finite attraction input");return Math.max(0,Math.min(100,value));}
    public record Inputs(double housing,double food,double safety,double civility,double activity){public Inputs{housing=clamp(housing);food=clamp(food);safety=clamp(safety);civility=clamp(civility);activity=clamp(activity);}}
    public record Result(double score,double appearanceChance,Map<String,Double>components){}
}
