package com.seaquake6324.civitas.application;
import com.seaquake6324.civitas.application.port.PopulationRepository;import com.seaquake6324.civitas.domain.population.CitizenRecord;import java.util.UUID;
/** Revision-checked health writeback from the visible entity adapter. */
public final class UpdateCitizenHealthService{
 public Result update(PopulationRepository repository,UUID id,long expectedRevision,int health){CitizenRecord current=repository.citizen(id).orElse(null);if(current==null)return new Result(false,true,null);if(current.revision()!=expectedRevision)return new Result(false,true,current);if(!current.alive())return new Result(false,false,current);int value=Math.max(0,Math.min(100,health));if(current.health()==value)return new Result(true,false,current);CitizenRecord updated=current.withHealth(value);repository.putCitizen(updated);return new Result(true,false,updated);}
 public record Result(boolean success,boolean stale,CitizenRecord citizen){}
}
