package com.seaquake6324.civitas.domain.population;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public record PopulationSummary(UUID cityId,int total,Map<Gender,Integer> genders,
        Map<AgeStage,Integer> ages,Map<CitizenRace,Integer> races,Map<CitizenRuntimeState,Integer> runtimeStates){
    public PopulationSummary{genders=Map.copyOf(genders);ages=Map.copyOf(ages);races=Map.copyOf(races);runtimeStates=Map.copyOf(runtimeStates);}
    public static PopulationSummary from(UUID cityId,Collection<CitizenRecord> citizens,AgeRules rules){
        EnumMap<Gender,Integer> genders=new EnumMap<>(Gender.class);EnumMap<AgeStage,Integer> ages=new EnumMap<>(AgeStage.class);
        EnumMap<CitizenRace,Integer> races=new EnumMap<>(CitizenRace.class);EnumMap<CitizenRuntimeState,Integer> states=new EnumMap<>(CitizenRuntimeState.class);int total=0;
        for(CitizenRecord citizen:citizens){if(!citizen.alive()||!cityId.equals(citizen.cityId()))continue;total++;genders.merge(citizen.gender(),1,Integer::sum);ages.merge(rules.evaluate(citizen.ageTicks()).stage(),1,Integer::sum);races.merge(citizen.race(),1,Integer::sum);states.merge(citizen.runtimeState(),1,Integer::sum);}
        return new PopulationSummary(cityId,total,genders,ages,races,states);
    }
}
