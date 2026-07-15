package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.PopulationRepository;
import com.seaquake6324.civitas.domain.population.CitizenLocationRecord;
import java.util.UUID;

/** Commits a main-thread physical observation without advancing virtual position. */
public final class UpdateCitizenLocationService {
    public enum Failure { NONE, MISSING_CITIZEN, DEAD, UNSETTLED, STALE }
    public record Result(CitizenLocationRecord location, Failure failure) { public boolean success(){return failure==Failure.NONE;} }
    public Result update(PopulationRepository repository, UUID citizenId, long expectedCitizenRevision,
            String dimension, long position, long now) {
        var citizen=repository.citizen(citizenId).orElse(null);
        if(citizen==null)return new Result(null,Failure.MISSING_CITIZEN);
        if(!citizen.alive())return new Result(null,Failure.DEAD);
        if(citizen.cityId()==null)return new Result(null,Failure.UNSETTLED);
        var previous=repository.location(citizenId).orElse(null);
        if(previous!=null&&previous.dimension().equals(dimension)&&previous.position()==position)return new Result(previous,Failure.NONE);
        long previousRevision=previous==null?0:previous.revision();
        CitizenLocationRecord next=new CitizenLocationRecord(citizenId,citizen.cityId(),dimension,position,now,previousRevision+1);
        return repository.updateLocation(next,expectedCitizenRevision,previousRevision)
                ?new Result(next,Failure.NONE):new Result(previous,Failure.STALE);
    }
}
