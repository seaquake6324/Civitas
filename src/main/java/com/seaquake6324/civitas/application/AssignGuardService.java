package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.PatrolAssignmentRepository;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.building.*;
import com.seaquake6324.civitas.domain.population.*;
import com.seaquake6324.civitas.domain.security.*;
import java.util.UUID;

/** Revision-checked guard appointment; world equipment evidence is supplied by the main-thread adapter. */
public final class AssignGuardService {
    private final GuardAssignmentRules rules = new GuardAssignmentRules();

    public Result assign(PatrolAssignmentRepository repository, Request request) {
        if(repository==null||request==null)return new Result(false,Failure.INVALID,null);
        City city=request.city();CitizenRecord citizen=request.citizen();PatrolRoute route=request.route();BuildingRecord post=request.guardPost();
        if(city.revision()!=request.expectedCityRevision())return new Result(false,Failure.STALE_CITY,null);
        if(!city.mayManage(request.actor()))return new Result(false,Failure.NOT_AUTHORIZED,null);
        if(citizen.revision()!=request.expectedCitizenRevision())return new Result(false,Failure.STALE_CITIZEN,null);
        if(!citizen.alive()||!city.id().equals(citizen.cityId()))return new Result(false,Failure.INVALID_CITIZEN,null);
        if(request.ageStage()==AgeStage.CHILD)return new Result(false,Failure.UNDERAGE,null);
        if(route==null||route.status()!=PatrolRoute.Status.VALID||!city.id().equals(route.cityId()))return new Result(false,Failure.INVALID_ROUTE,null);
        if(post==null||post.status()!=BuildingStatus.VALID||post.purpose()!=BuildingPurpose.GUARD_POST||!route.guardPostId().equals(post.id()))return new Result(false,Failure.INVALID_POST,null);
        if(repository.patrolAssignmentForCitizen(citizen.id()).isPresent())return new Result(false,Failure.ALREADY_ASSIGNED,null);
        GuardWillingness willingness=rules.evaluate(new GuardAssignmentRules.Inputs(citizen.settlementWillingness(),citizen.needs().safety(),request.equipment(),request.shift(),request.dutyReliability()),request.weights());
        boolean unwilling=willingness.score()<request.minimumWillingness();
        if(unwilling&&!request.force())return new Result(false,Failure.LOW_WILLINGNESS,null);
        PatrolAssignment.Status status=request.equipment().basicWeapon()?PatrolAssignment.Status.OFF_SHIFT:PatrolAssignment.Status.UNEQUIPPED;
        String reason=status==PatrolAssignment.Status.UNEQUIPPED?"missing_basic_weapon":"waiting_for_shift";
        PatrolAssignment assignment=new PatrolAssignment(request.assignmentId(),city.id(),route.id(),route.guardPostId(),citizen.id(),request.shift(),unwilling,willingness,status,0,true,request.now(),request.now(),0,1,reason);
        return repository.createPatrolAssignment(assignment,request.cityAssignmentCap())?new Result(true,null,assignment):new Result(false,Failure.CAP_OR_SHIFT_OCCUPIED,null);
    }

    public record Request(UUID assignmentId,City city,UUID actor,CitizenRecord citizen,long expectedCityRevision,
            long expectedCitizenRevision,AgeStage ageStage,PatrolRoute route,BuildingRecord guardPost,
            GuardShift shift,GuardEquipment equipment,boolean force,double dutyReliability,double minimumWillingness,
            GuardAssignmentRules.Weights weights,long now,int cityAssignmentCap) {
        public Request { if(assignmentId==null||city==null||actor==null||citizen==null||ageStage==null||shift==null||equipment==null||weights==null||now<0||cityAssignmentCap<1)throw new IllegalArgumentException("invalid guard appointment"); }
    }
    public enum Failure { INVALID,STALE_CITY,NOT_AUTHORIZED,STALE_CITIZEN,INVALID_CITIZEN,UNDERAGE,INVALID_ROUTE,INVALID_POST,ALREADY_ASSIGNED,LOW_WILLINGNESS,CAP_OR_SHIFT_OCCUPIED }
    public record Result(boolean success,Failure failure,PatrolAssignment assignment) {}
}
