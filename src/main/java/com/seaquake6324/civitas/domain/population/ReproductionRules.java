package com.seaquake6324.civitas.domain.population;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.Map;

/** Pure, explainable first-pass fertility and soft-condition rules. */
public final class ReproductionRules {
    public static final long DEFAULT_PREGNANCY_TICKS = 72_000;
    public static final long DEFAULT_BIRTH_COOLDOWN_TICKS = 168_000;
    public static final long DEFAULT_ATTEMPT_INTERVAL_TICKS = 24_000;
    public static final Weights DEFAULT_WEIGHTS = new Weights(.20,.20,.15,.10,.10,.10,.05,.10);

    public Result evaluate(Participant first, Participant second, Conditions conditions) {
        return evaluate(first,second,conditions,DEFAULT_WEIGHTS);
    }

    public Result evaluate(Participant first, Participant second, Conditions conditions, Weights weights) {
        if (first == null || second == null || conditions == null || weights == null) throw new IllegalArgumentException("Missing reproduction input");
        boolean biologicalPair = first.alive() && second.alive() && first.member() != null && second.member() != null
                && !first.member().equals(second.member()) && first.gender() != second.gender()
                && fertility(first.ageStage()) > 0 && fertility(second.ageStage()) > 0;
        LinkedHashMap<String, Double> components = new LinkedHashMap<>();
        double totalWeight=weights.total();
        add(components, "housing", conditions.housing(), weights.housing()/totalWeight);
        add(components, "food", conditions.food(), weights.food()/totalWeight);
        add(components, "safety", conditions.safety(), weights.safety()/totalWeight);
        add(components, "civility", conditions.civility(), weights.civility()/totalWeight);
        add(components, "activity", conditions.activity(), weights.activity()/totalWeight);
        add(components, "family_stability", conditions.familyStability(), weights.familyStability()/totalWeight);
        add(components, "casualty_recovery", conditions.casualtyRecovery(), weights.casualtyRecovery()/totalWeight);
        add(components, "uncrowded", conditions.uncrowded(), weights.uncrowded()/totalWeight);
        double conditionScore = components.values().stream().mapToDouble(Double::doubleValue).sum();
        double stageModifier = fertility(first.ageStage()) * fertility(second.ageStage());
        double consent = Math.min(first.willingness(), second.willingness()) / 100.0;
        double effectiveWillingness = biologicalPair ? clamp(conditionScore * stageModifier * consent) : 0;
        double speed = Math.max(.10, effectiveWillingness / 100.0);
        long interval = Math.max(1, Math.round(DEFAULT_ATTEMPT_INTERVAL_TICKS / speed));
        return new Result(biologicalPair, stageModifier, conditionScore, consent, effectiveWillingness,
                interval, conditions, weights, components, biologicalPair ? "eligible" : "biological_pair_required");
    }

    public double fertility(AgeStage stage) {
        if (stage == null) return 1.0; // Players currently have no persisted age and use the young-adult baseline.
        return switch (stage) {
            case CHILD -> 0.0;
            case ADOLESCENT -> 0.70;
            case YOUNG_ADULT -> 1.0;
            case MATURE_ADULT -> 0.80;
            case ELDER -> 0.50;
        };
    }

    private static void add(Map<String, Double> values, String key, double input, double weight) {
        values.put(key, clamp(input) * weight);
    }

    private static double clamp(double value) {
        if (!Double.isFinite(value)) throw new IllegalArgumentException("Non-finite reproduction input");
        return Math.max(0, Math.min(100, value));
    }

    public record Participant(FamilyMemberRef member, Gender gender, AgeStage ageStage,
                              boolean alive, double willingness) {
        public Participant {
            if (member == null || gender == null) throw new IllegalArgumentException("Missing participant identity");
            willingness = clamp(willingness);
        }
    }

    public record Conditions(double housing, double food, double safety, double civility,
                             double activity, double familyStability, double casualtyRecovery,
                             double uncrowded) {
        public Conditions {
            housing=clamp(housing);food=clamp(food);safety=clamp(safety);civility=clamp(civility);
            activity=clamp(activity);familyStability=clamp(familyStability);
            casualtyRecovery=clamp(casualtyRecovery);uncrowded=clamp(uncrowded);
        }
    }

    public record Weights(double housing,double food,double safety,double civility,double activity,
                          double familyStability,double casualtyRecovery,double uncrowded) {
        public Weights { validWeight(housing);validWeight(food);validWeight(safety);validWeight(civility);
            validWeight(activity);validWeight(familyStability);validWeight(casualtyRecovery);validWeight(uncrowded);
            if(housing+food+safety+civility+activity+familyStability+casualtyRecovery+uncrowded<=0)throw new IllegalArgumentException("At least one reproduction weight is required"); }
        public double total(){return housing+food+safety+civility+activity+familyStability+casualtyRecovery+uncrowded;}
        private static void validWeight(double value){if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Invalid reproduction weight");}
    }

    public record Result(boolean biologicallyEligible, double stageModifier, double conditionScore,
                         double consentModifier, double effectiveWillingness, long attemptIntervalTicks,
                         Conditions conditions,Weights weights,Map<String, Double> components, String reason) {
        public Result { if(!Double.isFinite(stageModifier)||!Double.isFinite(conditionScore)||!Double.isFinite(consentModifier)||!Double.isFinite(effectiveWillingness)||attemptIntervalTicks<1||conditions==null||weights==null||components==null||reason==null||reason.isBlank()||components.values().stream().anyMatch(v->v==null||!Double.isFinite(v)))throw new IllegalArgumentException("Invalid reproduction result");components = Collections.unmodifiableMap(new LinkedHashMap<>(components)); }
    }
}
