package com.seaquake6324.civitas.domain.population;

import java.util.LinkedHashMap;
import java.util.Map;

/** Explainable household pressure to leave a city; conditions affect probability, not identity eligibility. */
public final class OutMigrationRules {
    public Result evaluate(Inputs in,double maximumChance){if(in==null||!Double.isFinite(maximumChance)||maximumChance<0||maximumChance>1)throw new IllegalArgumentException("invalid out-migration input");LinkedHashMap<String,Double>parts=new LinkedHashMap<>();add(parts,"migration_willingness",in.migrationWillingness(),.35,false);add(parts,"settlement_doubt",in.settlementWillingness(),.15,true);add(parts,"housing_pressure",in.housing(),.15,true);add(parts,"food_pressure",in.food(),.10,true);add(parts,"safety_pressure",in.safety(),.10,true);add(parts,"low_civility",in.civility(),.075,true);add(parts,"low_activity",in.activity(),.075,true);double score=parts.values().stream().mapToDouble(Double::doubleValue).sum();return new Result(score,maximumChance*score/100.0,Map.copyOf(parts));}
    private static void add(Map<String,Double>parts,String key,double raw,double weight,boolean inverse){double value=clamp(raw);parts.put(key,(inverse?100-value:value)*weight);}
    private static double clamp(double value){if(!Double.isFinite(value))throw new IllegalArgumentException("non-finite out-migration input");return Math.max(0,Math.min(100,value));}
    public record Inputs(double migrationWillingness,double settlementWillingness,double housing,double food,double safety,double civility,double activity){public Inputs{migrationWillingness=clamp(migrationWillingness);settlementWillingness=clamp(settlementWillingness);housing=clamp(housing);food=clamp(food);safety=clamp(safety);civility=clamp(civility);activity=clamp(activity);}}
    public record Result(double score,double chance,Map<String,Double>components){}
}
