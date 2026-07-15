package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.ReproductionRepository;
import com.seaquake6324.civitas.domain.population.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Creates a real persisted pregnancy after pure eligibility and revision checks. */
public final class ConceiveChildService {
    private final ReproductionRules rules=new ReproductionRules();
    private final InfidelityRules infidelityRules=new InfidelityRules();

    public Result conceive(ReproductionRepository repository,Request request){
        ReproductionRules.Result evaluation=rules.evaluate(request.first(),request.second(),request.conditions(),request.weights());
        if(!evaluation.biologicallyEligible())return new Result(false,Failure.INELIGIBLE,null,evaluation);
        FamilyMemberRef carrier=request.first().gender()==Gender.FEMALE?request.first().member():request.second().member();
        boolean infidelity=infidelityRules.isInfidelity(request.first().member(),request.second().member(),request.firstSpouses(),request.secondSpouses());
        PregnancyRecord pregnancy=new PregnancyRecord(request.pregnancyId(),request.cityId(),Set.of(request.first().member(),request.second().member()),carrier,request.now(),Math.addExact(request.now(),request.pregnancyTicks()),infidelity,evaluation.effectiveWillingness(),evaluation,1);
        Map<FamilyMemberRef,Long>expected=Map.of(request.first().member(),request.firstRevision(),request.second().member(),request.secondRevision());
        if(!repository.createPregnancy(pregnancy,expected,request.pregnancyCapacity(),request.now()))return new Result(false,Failure.STALE_OR_CAPACITY,null,evaluation);
        return new Result(true,null,pregnancy,evaluation);
    }

    public record Request(UUID pregnancyId,UUID cityId,ReproductionRules.Participant first,long firstRevision,
                          ReproductionRules.Participant second,long secondRevision,ReproductionRules.Conditions conditions,
                          ReproductionRules.Weights weights,
                          Set<FamilyMemberRef> firstSpouses,Set<FamilyMemberRef> secondSpouses,long now,
                          long pregnancyTicks,int pregnancyCapacity){
        public Request{firstSpouses=Set.copyOf(firstSpouses);secondSpouses=Set.copyOf(secondSpouses);if(pregnancyId==null||cityId==null||weights==null||pregnancyTicks<1||pregnancyCapacity<1||now<0)throw new IllegalArgumentException("Invalid conception request");}
    }
    public enum Failure{INELIGIBLE,STALE_OR_CAPACITY}
    public record Result(boolean success,Failure failure,PregnancyRecord pregnancy,ReproductionRules.Result evaluation){}
}
