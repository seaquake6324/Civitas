package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.domain.population.AgeRules;
import com.seaquake6324.civitas.domain.population.CitizenRecord;
import com.seaquake6324.civitas.domain.population.LifespanRules;

/** Advances age and applies the already-persisted deterministic old-age limit. */
public final class PopulationAgingService {
    public Result advance(CitizenRecord citizen,long now,AgeRules rules,LifespanRules lifespanRules){
        AgeRules.Result before=rules.evaluate(citizen.ageTicks());CitizenRecord updated=citizen.alive()?citizen.advanceAge(now):citizen;
        AgeRules.Result after=rules.evaluate(updated.ageTicks());boolean died=lifespanRules.expired(updated,rules);if(died)updated=updated.dieOfOldAge(now);return new Result(updated,before,after,died);
    }
    public Result advance(CitizenRecord citizen,long now,AgeRules rules){return advance(citizen,now,rules,new LifespanRules(60,90));}
    public record Result(CitizenRecord citizen,AgeRules.Result before,AgeRules.Result after,boolean died){}
}
