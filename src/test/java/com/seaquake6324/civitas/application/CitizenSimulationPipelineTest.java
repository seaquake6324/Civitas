package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.application.port.*;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.*;
import com.seaquake6324.civitas.domain.population.*;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class CitizenSimulationPipelineTest {
    private final AgeRules ages=new AgeRules(100,12,18,35,60);
    private final LifespanRules lifespans=new LifespanRules(60,90);

    @Test void immutableSnapshotCanBePlannedOffThreadAndAppliedAfterAllRevisionsMatch() throws Exception {
        City city=city();CitizenRecord citizen=citizen(city.id(),1800,100);MemoryPopulation population=new MemoryPopulation(citizen);MemoryCities cities=new MemoryCities(city);MemoryBuildings buildings=new MemoryBuildings();
        ArrayList<CitizenSimulationSnapshot.BuildingRevision> mutable=new ArrayList<>();
        CitizenSimulationSnapshot snapshot=new CitizenSimulationSnapshot(citizen,city.id(),city.revision(),mutable,200,ages,lifespans);mutable.add(new CitizenSimulationSnapshot.BuildingRevision(UUID.randomUUID(),1));assertTrue(snapshot.buildingRevisions().isEmpty());
        var executor=Executors.newSingleThreadExecutor();CitizenIntent intent;try{intent=executor.submit(()->new PlanCitizenSimulationService().plan(snapshot).orElseThrow()).get(2,TimeUnit.SECONDS);}finally{executor.shutdownNow();}
        var result=new ApplyCitizenIntentService().apply(population,cities,buildings,intent);assertTrue(result.success());assertEquals(1900,result.citizen().ageTicks());assertEquals(200,result.citizen().lastAgeUpdateTick());assertEquals(citizen.revision()+1,result.citizen().revision());
    }

    @Test void staleCitizenCityAndBuildingRevisionsFailClosed(){
        City city=city();CitizenRecord citizen=citizen(city.id(),1800,100);BuildingRecord building=building(city.id());
        CitizenSimulationSnapshot snapshot=new CitizenSimulationSnapshot(citizen,city.id(),city.revision(),List.of(new CitizenSimulationSnapshot.BuildingRevision(building.id(),building.revision())),200,ages,lifespans);
        CitizenIntent intent=new PlanCitizenSimulationService().plan(snapshot).orElseThrow();
        MemoryPopulation population=new MemoryPopulation(citizen.withGender(Gender.FEMALE));MemoryCities cities=new MemoryCities(city);MemoryBuildings buildings=new MemoryBuildings(building);
        assertEquals(ApplyCitizenIntentService.Failure.STALE_CITIZEN,new ApplyCitizenIntentService().apply(population,cities,buildings,intent).failure());
        population=new MemoryPopulation(citizen);cities=new MemoryCities(city.updateIdentity("Changed",0));assertEquals(ApplyCitizenIntentService.Failure.STALE_CITY,new ApplyCitizenIntentService().apply(population,cities,buildings,intent).failure());
        cities=new MemoryCities(city);buildings=new MemoryBuildings(building.stale("changed"));assertEquals(ApplyCitizenIntentService.Failure.STALE_BUILDING,new ApplyCitizenIntentService().apply(population,cities,buildings,intent).failure());
    }

    @Test void replacementThatWasNotProducedByPureAgeRuleIsRejected(){
        City city=city();CitizenRecord citizen=citizen(city.id(),1800,100);CitizenIntent valid=new PlanCitizenSimulationService().plan(new CitizenSimulationSnapshot(citizen,city.id(),city.revision(),List.of(),200,ages,lifespans)).orElseThrow();
        CitizenIntent forged=new CitizenIntent(valid.type(),valid.citizenId(),valid.expectedCitizenRevision(),valid.cityId(),valid.expectedCityRevision(),valid.expectedBuildings(),valid.snapshotAt(),valid.ageRules(),valid.lifespanRules(),citizen.withGender(Gender.FEMALE),valid.beforeStage(),valid.afterStage(),false);
        var result=new ApplyCitizenIntentService().apply(new MemoryPopulation(citizen),new MemoryCities(city),new MemoryBuildings(),forged);assertEquals(ApplyCitizenIntentService.Failure.INVALID_INTENT,result.failure());
    }

    @Test void missingBuildingObservationMustStillMatchAtCommit(){City city=city();CitizenRecord citizen=citizen(city.id(),1800,100);BuildingRecord appeared=building(city.id());CitizenSimulationSnapshot snapshot=new CitizenSimulationSnapshot(citizen,city.id(),city.revision(),List.of(CitizenSimulationSnapshot.BuildingRevision.missing(appeared.id())),200,ages,lifespans);CitizenIntent intent=new PlanCitizenSimulationService().plan(snapshot).orElseThrow();assertTrue(new ApplyCitizenIntentService().apply(new MemoryPopulation(citizen),new MemoryCities(city),new MemoryBuildings(),intent).success());var stale=new ApplyCitizenIntentService().apply(new MemoryPopulation(citizen),new MemoryCities(city),new MemoryBuildings(appeared),intent);assertEquals(ApplyCitizenIntentService.Failure.STALE_BUILDING,stale.failure());}

    @Test void oldAgeDeathIsCalculatedOffThreadButOnlyCommittedByApplicationBoundary(){City city=city();CitizenRecord citizen=new CitizenRecord(UUID.randomUUID(),"Mira","North",CitizenRace.HUMAN,"human_1",Gender.FEMALE,6999,100,city.id(),null,null,null,"",80,new CitizenNeeds(50,50,50,50),Map.of(),Set.of(),50,0,CitizenRuntimeState.MATERIALIZED,0,70,0,4);CitizenSimulationSnapshot snapshot=new CitizenSimulationSnapshot(citizen,city.id(),city.revision(),List.of(),200,ages,lifespans);CitizenIntent intent=new PlanCitizenSimulationService().plan(snapshot).orElseThrow();assertTrue(intent.permanentDeath());assertFalse(intent.replacement().alive());assertTrue(citizen.alive());MemoryPopulation population=new MemoryPopulation(citizen);var applied=new ApplyCitizenIntentService().apply(population,new MemoryCities(city),new MemoryBuildings(),intent);assertTrue(applied.success());assertFalse(population.citizen(citizen.id()).orElseThrow().alive());assertEquals(CitizenRuntimeState.VIRTUAL,applied.citizen().runtimeState());}

    private static City city(){UUID leader=UUID.randomUUID();return new City(UUID.randomUUID(),"City",0,"minecraft:overworld",0,0,0,leader,leader,Set.of(leader),Set.of(0L)).updateIdentity("City",0);}
    private static CitizenRecord citizen(UUID city,long age,long updated){return new CitizenRecord(UUID.randomUUID(),"Luca","Rossi",CitizenRace.HUMAN,"human_0",Gender.MALE,age,updated,city,null,null,null,"",100,new CitizenNeeds(50,50,50,50),Map.of(),Set.of(),50,0,CitizenRuntimeState.VIRTUAL,0,70,0,3);}
    private static BuildingRecord building(UUID city){return new BuildingRecord(UUID.randomUUID(),city,"minecraft:overworld",BuildingPurpose.RESIDENCE,0,0,Set.of(0L),Map.of(),BuildingFeatures.EMPTY,0,BuildingStatus.VALID,2,0,"");}
    private static final class MemoryPopulation implements PopulationRepository{private final Map<UUID,CitizenRecord>values=new HashMap<>();MemoryPopulation(CitizenRecord citizen){values.put(citizen.id(),citizen);}public Optional<PlayerCivitasProfile>profile(UUID id){return Optional.empty();}public void putProfile(PlayerCivitasProfile value){}public Optional<CitizenRecord>citizen(UUID id){return Optional.ofNullable(values.get(id));}public Collection<CitizenRecord>citizens(){return values.values();}public void putCitizen(CitizenRecord value){values.put(value.id(),value);}public Optional<Household>household(UUID id){return Optional.empty();}public Collection<Household>households(){return List.of();}public void putHousehold(Household value){}public boolean createHousehold(Household value,Map<UUID,Long>expected){return false;}public Optional<Household>householdForPartner(FamilyMemberRef ref){return Optional.empty();}public Optional<MarriageProposal>proposal(UUID id){return Optional.empty();}public Optional<MarriageProposal>activeProposalFor(FamilyMemberRef ref){return Optional.empty();}public int proposalCount(){return 0;}public void putProposal(MarriageProposal proposal){}}
    private static final class MemoryCities implements CityRepository{private final Map<UUID,City>values=new HashMap<>();MemoryCities(City city){values.put(city.id(),city);}public Collection<City>cities(){return values.values();}public Optional<City>byName(String name){return Optional.empty();}public Optional<City>cityLedBy(UUID id){return Optional.empty();}public void add(City city){values.put(city.id(),city);}}
    private static final class MemoryBuildings implements BuildingRepository{private final Map<UUID,BuildingRecord>values=new HashMap<>();MemoryBuildings(BuildingRecord...records){for(var record:records)values.put(record.id(),record);}public Collection<BuildingRecord>forCity(UUID city){return values.values().stream().filter(record->record.cityId().equals(city)).toList();}public Optional<BuildingRecord>byId(UUID id){return Optional.ofNullable(values.get(id));}public void put(BuildingRecord record){values.put(record.id(),record);}}
}
