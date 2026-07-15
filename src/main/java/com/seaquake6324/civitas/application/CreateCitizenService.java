package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.PopulationRepository;
import com.seaquake6324.civitas.domain.population.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Creates only from explicit caller inputs; recruitment, birth and migration rules remain separate gates. */
public final class CreateCitizenService{
 public record Request(UUID id,String givenName,String familyName,CitizenRace race,String appearanceKey,Gender gender,long ageTicks,long now,UUID cityId,int lifespanYears){public Request(UUID id,String givenName,String familyName,CitizenRace race,String appearanceKey,Gender gender,long ageTicks,long now,UUID cityId){this(id,givenName,familyName,race,appearanceKey,gender,ageTicks,now,cityId,new LifespanRules(60,90).years(id));}}
 public CitizenRecord create(PopulationRepository repository,Request request){if(repository.citizen(request.id()).isPresent())throw new IllegalArgumentException("duplicate citizen");CitizenRecord citizen=new CitizenRecord(request.id(),request.givenName(),request.familyName(),request.race(),request.appearanceKey(),request.gender(),request.ageTicks(),request.now(),request.cityId(),null,null,null,"",100,CitizenNeeds.neutral(),Map.of(),Set.of(),50,0,CitizenRuntimeState.VIRTUAL,0,request.lifespanYears(),0,1);repository.putCitizen(citizen);return citizen;}
}
