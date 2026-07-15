package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.ReproductionRepository;
import com.seaquake6324.civitas.domain.population.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Builds and atomically commits an NPC child when a persisted pregnancy becomes due. */
public final class CompleteBirthService {
    private final ChildIdentityRules identityRules=new ChildIdentityRules();

    public Result complete(ReproductionRepository repository,Request request){
        PregnancyRecord pregnancy=repository.pregnancy(request.pregnancyId()).orElse(null);
        if(pregnancy==null)return new Result(false,Failure.MISSING,null,null,null);
        if(pregnancy.revision()!=request.expectedPregnancyRevision())return new Result(false,Failure.STALE,null,null,pregnancy);
        if(request.now()<pregnancy.dueAt())return new Result(false,Failure.NOT_DUE,null,null,pregnancy);
        Map<FamilyMemberRef,Long>revisions=new HashMap<>();Map<FamilyMemberRef,CitizenRace>races=new HashMap<>();
        for(FamilyMemberRef parent:pregnancy.parents()){
            if(parent.kind()==FamilyMemberRef.Kind.PLAYER){PlayerCivitasProfile profile=repository.profile(parent.id()).orElse(null);if(profile==null)return new Result(false,Failure.MISSING_PARENT,null,null,pregnancy);revisions.put(parent,profile.revision());}
            else{CitizenRecord citizen=repository.citizen(parent.id()).orElse(null);if(citizen==null)return new Result(false,Failure.MISSING_PARENT,null,null,pregnancy);revisions.put(parent,citizen.revision());races.put(parent,citizen.race());}
        }
        if(pregnancy.gestationalParent().kind()==FamilyMemberRef.Kind.CITIZEN&&!repository.citizen(pregnancy.gestationalParent().id()).orElseThrow().alive())return new Result(false,Failure.CARRIER_DIED,null,null,pregnancy);
        ChildIdentityRules.Result identity=identityRules.choose(pregnancy,races);
        CitizenNameGenerator.Name name=CitizenNameGenerator.generate(request.childId().getMostSignificantBits()^request.childId().getLeastSignificantBits(),identity.gender());
        Household current=repository.householdForGuardian(pregnancy.gestationalParent()).or(()->repository.householdForPartner(pregnancy.gestationalParent())).orElse(null);
        Set<FamilyMemberRef>guardians=current!=null&&current.partners().containsAll(pregnancy.parents())?pregnancy.parents():Set.of(pregnancy.gestationalParent());
        UUID residence=current==null?carrierResidence(repository,pregnancy.gestationalParent()):current.residenceId();
        Household household=current==null
                ?new Household(request.householdId(),pregnancy.cityId(),Set.of(),Set.of(),guardians,Set.of(request.childId()),residence,0,0,1)
                :current.addChild(request.childId(),guardians);
        CitizenRecord child=new CitizenRecord(request.childId(),name.given(),name.family(),identity.race(),identity.appearanceKey(),identity.gender(),0,request.now(),pregnancy.cityId(),household.id(),residence,null,"",100,CitizenNeeds.neutral(),Map.of(),Set.of(),50,0,CitizenRuntimeState.VIRTUAL,0,request.lifespanYears(),0,1);
        ParentageRecord parentage=new ParentageRecord(child.id(),pregnancy.cityId(),pregnancy.parents(),request.now(),pregnancy.infidelity(),ParentageRecord.Source.BIRTH,1);
        boolean committed=repository.commitBirth(pregnancy,child,parentage,household,revisions,Math.addExact(request.now(),request.birthCooldownTicks()),request.householdChildSafetyCap());
        return committed?new Result(true,null,child,household,pregnancy):new Result(false,Failure.STALE,null,null,pregnancy);
    }

    private static UUID carrierResidence(ReproductionRepository repository,FamilyMemberRef carrier){return carrier.kind()==FamilyMemberRef.Kind.CITIZEN?repository.citizen(carrier.id()).map(CitizenRecord::residenceId).orElse(null):null;}
    public record Request(UUID pregnancyId,long expectedPregnancyRevision,UUID childId,UUID householdId,long now,
                          int lifespanYears,long birthCooldownTicks,int householdChildSafetyCap){public Request{if(pregnancyId==null||childId==null||householdId==null||now<0||lifespanYears<1||birthCooldownTicks<0||householdChildSafetyCap<1)throw new IllegalArgumentException("Invalid birth request");}}
    public enum Failure{MISSING,STALE,NOT_DUE,MISSING_PARENT,CARRIER_DIED}
    public record Result(boolean success,Failure failure,CitizenRecord child,Household household,PregnancyRecord pregnancy){}
}
