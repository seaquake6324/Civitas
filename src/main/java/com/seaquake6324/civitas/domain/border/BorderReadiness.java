package com.seaquake6324.civitas.domain.border;
public record BorderReadiness(double civility, double activity, double safeConstruction, double fortification,
        double civilityPart, double activityPart, double safetyPart, double fortificationPart, double total) {
    public static BorderReadiness calculate(double civility,double activity,double safety,double fortification){
        return calculate(civility,activity,safety,fortification,new Weights(.30,.30,.20,.20));
    }
    public static BorderReadiness calculate(double civility,double activity,double safety,double fortification,Weights weights){
        double c=clamp(civility)*weights.civility,a=clamp(activity)*weights.activity,s=clamp(safety)*weights.safety,f=clamp(fortification)*weights.fortification;
        return new BorderReadiness(clamp(civility),clamp(activity),clamp(safety),clamp(fortification),c,a,s,f,c+a+s+f);
    }
    public record Weights(double civility,double activity,double safety,double fortification){public Weights{civility=Math.max(0,civility);activity=Math.max(0,activity);safety=Math.max(0,safety);fortification=Math.max(0,fortification);}}
    private static double clamp(double v){return Math.max(0,Math.min(100,v));}
}
