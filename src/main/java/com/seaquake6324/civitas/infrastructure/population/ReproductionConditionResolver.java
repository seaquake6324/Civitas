package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.*;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import com.seaquake6324.civitas.domain.population.*;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.ChunkPos;

/** Main-thread adapter that resolves every soft reproduction input from bounded world-backed records. */
public final class ReproductionConditionResolver {
    private static long resolutions,citizensExamined,buildingsExamined,truncatedResolutions;private static Result last;
    public static Result resolve(MinecraftServer server,City city,FamilyMemberRef first,CitizenRecord firstCitizen,FamilyMemberRef second,CitizenRecord secondCitizen,long now){
        PopulationSavedData population=PopulationSavedData.get(server);BuildingSavedData buildings=BuildingSavedData.get(server);CivilizationSavedData civilization=CivilizationSavedData.get(server);ThreatSavedData threat=ThreatSavedData.get(server);int cap=CivitasConfig.REPRODUCTION_CONDITION_SCAN_LIMIT.get();
        Household household=sharedHousehold(population,first,second);BuildingRecord residence=residence(buildings,household,firstCitizen,secondCitizen);long evidenceChunk=residence==null?city.coreChunk():ChunkPos.pack(net.minecraft.core.BlockPos.of(residence.interior()));String dimension=residence==null?city.dimension():residence.dimension();
        double housing=household==null?(residence==null?25:100):household.housingCoverage();double food=household==null?averageNeeds(firstCitizen,secondCitizen,true):household.foodCoverage();
        var cell=threat.securityCell(city.id(),evidenceChunk).orElse(null);double citySafety=cell==null?50:100-cell.diagnosticRisk();double safety=averageNeeds(firstCitizen,secondCitizen,false);if(firstCitizen==null||secondCitizen==null)safety=(safety+citySafety)/2;
        var surface=civilization.get(dimension,evidenceChunk,CivilizationLayer.SURFACE);var underground=civilization.get(dimension,evidenceChunk,CivilizationLayer.UNDERGROUND);double civility=Math.max(surface.civility(),underground.civility()),activity=Math.max(surface.activity(),underground.activity());
        double family=spouses(population,first,second)?100:household!=null?75:50;
        var citizens=population.citizenBatch(null,cap);int alive=0,recentDeaths=0;for(CitizenRecord citizen:citizens.records())if(city.id().equals(citizen.cityId())){if(citizen.alive())alive++;else if(citizen.diedAtTick()>0&&now-citizen.diedAtTick()<=CivitasConfig.REPRODUCTION_CASUALTY_RECOVERY_TICKS.get())recentDeaths++;}
        var cityBuildings=buildings.cityPage(city.id(),cap);int capacity=cityBuildings.records().stream().filter(b->b.status()==BuildingStatus.VALID&&b.purpose()==BuildingPurpose.RESIDENCE).mapToInt(BuildingRecord::capacity).sum();
        double recovery=Math.max(20,100-recentDeaths*20.0),uncrowded=alive==0?100:Math.min(100,100.0*capacity/alive);boolean truncated=citizens.records().size()<population.citizenCount()||cityBuildings.truncated();
        Result result=new Result(new ReproductionRules.Conditions(housing,food,safety,civility,activity,family,recovery,uncrowded),citizens.examined(),cityBuildings.examined(),recentDeaths,alive,capacity,evidenceChunk,residence!=null,cell!=null,truncated);
        resolutions++;citizensExamined+=result.citizensExamined();buildingsExamined+=result.buildingsExamined();if(truncated)truncatedResolutions++;last=result;return result;
    }
    private static Household sharedHousehold(PopulationSavedData data,FamilyMemberRef a,FamilyMemberRef b){Household h=data.householdForPartner(a).or(()->data.householdForGuardian(a)).orElse(null);return h!=null&&(h.partners().contains(b)||h.guardians().contains(b))?h:null;}
    private static boolean spouses(PopulationSavedData data,FamilyMemberRef a,FamilyMemberRef b){return data.householdForPartner(a).map(h->h.partners().contains(b)).orElse(false);}
    private static BuildingRecord residence(BuildingSavedData buildings,Household h,CitizenRecord a,CitizenRecord b){UUID id=h!=null?h.residenceId():a!=null&&a.residenceId()!=null?a.residenceId():b==null?null:b.residenceId();return id==null?null:buildings.byId(id).filter(v->v.status()==BuildingStatus.VALID&&v.purpose()==BuildingPurpose.RESIDENCE).orElse(null);}
    private static double averageNeeds(CitizenRecord a,CitizenRecord b,boolean food){int n=0;double total=0;if(a!=null){total+=food?a.needs().food():a.needs().safety();n++;}if(b!=null){total+=food?b.needs().food():b.needs().safety();n++;}return n==0?50:total/n;}
    public static Metrics metrics(){return new Metrics(resolutions,citizensExamined,buildingsExamined,truncatedResolutions,last);}
    public record Result(ReproductionRules.Conditions conditions,int citizensExamined,int buildingsExamined,int recentDeaths,int aliveCitizens,int housingCapacity,long evidenceChunk,boolean residenceLinked,boolean securityLinked,boolean truncated){}
    public record Metrics(long resolutions,long citizensExamined,long buildingsExamined,long truncatedResolutions,Result last){}
    private ReproductionConditionResolver(){}
}
