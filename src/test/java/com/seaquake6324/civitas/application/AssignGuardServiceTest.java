package com.seaquake6324.civitas.application;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.application.port.PatrolAssignmentRepository;
import com.seaquake6324.civitas.domain.*;import com.seaquake6324.civitas.domain.building.*;import com.seaquake6324.civitas.domain.civilization.FacilityCategory;import com.seaquake6324.civitas.domain.population.*;import com.seaquake6324.civitas.domain.security.*;
import java.util.*;import org.junit.jupiter.api.Test;

class AssignGuardServiceTest {
    @Test void adolescentCanBeVoluntarilyAssignedAndMissingWeaponStaysInactive(){Fixture f=new Fixture();var result=f.assign(false,new GuardEquipment(false,4,true),AgeStage.ADOLESCENT,0);assertTrue(result.success());assertEquals(PatrolAssignment.Status.UNEQUIPPED,result.assignment().status());assertFalse(result.assignment().forced());}
    @Test void unwillingCitizenRequiresExplicitForceAndPreservesScore(){Fixture f=new Fixture();var refused=f.assign(false,new GuardEquipment(false,0,false),AgeStage.YOUNG_ADULT,99);assertEquals(AssignGuardService.Failure.LOW_WILLINGNESS,refused.failure());var forced=f.assign(true,new GuardEquipment(false,0,false),AgeStage.YOUNG_ADULT,99);assertTrue(forced.success());assertTrue(forced.assignment().forced());assertTrue(forced.assignment().willingness().score()<99);}
    @Test void childrenAndOrdinaryMembersCannotBeAssigned(){Fixture child=new Fixture();assertEquals(AssignGuardService.Failure.UNDERAGE,child.assign(false,new GuardEquipment(true,0,false),AgeStage.CHILD,0).failure());Fixture member=new Fixture();var request=member.request(member.member,false,new GuardEquipment(true,0,false),AgeStage.YOUNG_ADULT,0);assertEquals(AssignGuardService.Failure.NOT_AUTHORIZED,new AssignGuardService().assign(member.repo,request).failure());}

    private static final class Fixture {
        final UUID founder=UUID.randomUUID(),member=UUID.randomUUID();
        final City city=new City(UUID.randomUUID(),"Test",0,"minecraft:overworld",0,ChunkCoordinate.pack(0,0),1,founder,founder,Set.of(founder,member),Set.of(ChunkCoordinate.pack(0,0)));
        final CitizenRecord citizen=new CitizenRecord(UUID.randomUUID(),"Mira","North",CitizenRace.HUMAN,"human_0",Gender.FEMALE,20,0,city.id(),null,null,null,"",100,new CitizenNeeds(70,70,70,70),Map.of(),Set.of(),70,10,CitizenRuntimeState.MATERIALIZED,0,1);
        final BuildingRecord post=new BuildingRecord(UUID.randomUUID(),city.id(),city.dimension(),BuildingPurpose.GUARD_POST,10,11,Set.of(11L,12L),Map.of(FacilityCategory.PUBLIC,1),1,BuildingStatus.VALID,2,20,"");
        final PatrolRoute route=new PatrolRoute(UUID.randomUUID(),city.id(),city.dimension(),post.id(),List.of(11L,12L),founder,PatrolRoute.Status.VALID,20,1,"");final Memory repo=new Memory();
        AssignGuardService.Result assign(boolean force,GuardEquipment equipment,AgeStage age,double minimum){return new AssignGuardService().assign(repo,request(founder,force,equipment,age,minimum));}
        AssignGuardService.Request request(UUID actor,boolean force,GuardEquipment equipment,AgeStage age,double minimum){return new AssignGuardService.Request(UUID.randomUUID(),city,actor,citizen,city.revision(),citizen.revision(),age,route,post,GuardShift.DAY,equipment,force,50,minimum,new GuardAssignmentRules.Weights(.35,.15,.20,.10,.05,.05,.10),30,16);}
    }
    private static final class Memory implements PatrolAssignmentRepository {final Map<UUID,PatrolAssignment>values=new HashMap<>();public Optional<PatrolAssignment>patrolAssignment(UUID id){return Optional.ofNullable(values.get(id));}public Optional<PatrolAssignment>patrolAssignmentForCitizen(UUID id){return values.values().stream().filter(a->a.citizenId().equals(id)).findFirst();}public boolean createPatrolAssignment(PatrolAssignment a,int cap){if(values.size()>=cap||patrolAssignmentForCitizen(a.citizenId()).isPresent()||values.values().stream().anyMatch(x->x.routeId().equals(a.routeId())&&x.shift()==a.shift()))return false;values.put(a.id(),a);return true;}public boolean replacePatrolAssignment(PatrolAssignment a,long revision){if(values.get(a.id())==null||values.get(a.id()).revision()!=revision)return false;values.put(a.id(),a);return true;}}
}
