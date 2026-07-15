package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.BuildingRepository;
import com.seaquake6324.civitas.application.port.CityRepository;
import com.seaquake6324.civitas.application.port.PopulationRepository;
import com.seaquake6324.civitas.domain.population.CitizenIntent;
import com.seaquake6324.civitas.domain.population.CitizenRecord;

/** Server-thread commit boundary for background results. */
public final class ApplyCitizenIntentService {
    private final PopulationAgingService aging=new PopulationAgingService();
    public Result apply(PopulationRepository population,CityRepository cities,BuildingRepository buildings,CitizenIntent intent){
        CitizenRecord current=population.citizen(intent.citizenId()).orElse(null);
        if(current==null)return failure(Failure.MISSING_CITIZEN,null);
        if(current.revision()!=intent.expectedCitizenRevision())return failure(Failure.STALE_CITIZEN,current);
        if(!current.alive())return failure(Failure.PERMANENTLY_DEAD,current);
        var city=cities.byId(intent.cityId()).orElse(null);
        if(city==null)return failure(Failure.MISSING_CITY,current);
        if(city.revision()!=intent.expectedCityRevision())return failure(Failure.STALE_CITY,current);
        for(var expected:intent.expectedBuildings()){
            var building=buildings.byId(expected.id()).orElse(null);
            if(!expected.present()){if(building!=null)return failure(Failure.STALE_BUILDING,current);continue;}
            if(building==null)return failure(Failure.MISSING_BUILDING,current);
            if(building.revision()!=expected.revision())return failure(Failure.STALE_BUILDING,current);
        }
        CitizenRecord replacement=intent.replacement();
        var recalculated=aging.advance(current,intent.snapshotAt(),intent.ageRules(),intent.lifespanRules());
        if(!replacement.equals(recalculated.citizen())||intent.beforeStage()!=recalculated.before().stage()
                ||intent.afterStage()!=recalculated.after().stage()||intent.permanentDeath()!=recalculated.died())
            return failure(Failure.INVALID_INTENT,current);
        population.putCitizen(replacement);
        return new Result(true,null,replacement);
    }
    private static Result failure(Failure failure,CitizenRecord citizen){return new Result(false,failure,citizen);}
    public enum Failure { MISSING_CITIZEN,STALE_CITIZEN,PERMANENTLY_DEAD,MISSING_CITY,STALE_CITY,MISSING_BUILDING,STALE_BUILDING,INVALID_INTENT }
    public record Result(boolean success,Failure failure,CitizenRecord citizen){}
}
