package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.ReproductionRepository;
import com.seaquake6324.civitas.domain.population.*;
import java.util.HashSet;
import java.util.Set;

/** Turns only real, source-backed children with no surviving parent into orphans. */
public final class ReconcileOrphansAfterParentDeathService {
    public Result reconcile(ReproductionRepository repository,FamilyMemberRef deceased,long now,AgeRules ageRules,int limit){
        var records=repository.parentageForParent(deceased,Math.max(0,limit)+1);int examined=0,orphaned=0,keptWithParent=0,householdsUpdated=0,failures=0;boolean truncated=records.size()>Math.max(0,limit);
        for(ParentageRecord parentage:records){if(examined>=Math.max(0,limit))break;examined++;CitizenRecord child=repository.citizen(parentage.childId()).orElse(null);if(child==null||!child.alive()||ageRules.evaluate(child.ageTicks()).stage()!=AgeStage.CHILD)continue;
            Set<FamilyMemberRef>available=new HashSet<>();for(FamilyMemberRef parent:parentage.parents())if(available(repository,parent))available.add(parent);
            Household household=child.householdId()==null?null:repository.household(child.householdId()).orElse(null);
            if(!available.isEmpty()){
                keptWithParent++;
                if(household!=null){Set<FamilyMemberRef>local=new HashSet<>();for(FamilyMemberRef parent:available)if(local(repository,parent,household.id()))local.add(parent);if(!local.isEmpty()&&!local.equals(household.guardians()))try{repository.putHousehold(household.withGuardians(local));householdsUpdated++;}catch(RuntimeException exception){failures++;}}
                continue;
            }
            if(household!=null&&!household.guardians().isEmpty())try{repository.putHousehold(household.withGuardians(Set.of()));householdsUpdated++;}catch(RuntimeException exception){failures++;continue;}
            if(repository.orphan(child.id()).isEmpty())try{repository.putOrphan(new OrphanRecord(child.id(),parentage.cityId(),OrphanRecord.Reason.PARENTS_UNAVAILABLE,household==null?null:household.id(),household==null?child.residenceId():household.residenceId(),now,1));orphaned++;}catch(RuntimeException exception){failures++;}
        }
        return new Result(examined,orphaned,keptWithParent,householdsUpdated,failures,truncated);
    }
    private static boolean available(ReproductionRepository repository,FamilyMemberRef parent){return parent.kind()==FamilyMemberRef.Kind.PLAYER?repository.profile(parent.id()).isPresent():repository.citizen(parent.id()).map(CitizenRecord::alive).orElse(false);}
    private static boolean local(ReproductionRepository repository,FamilyMemberRef parent,java.util.UUID householdId){return parent.kind()==FamilyMemberRef.Kind.PLAYER||repository.citizen(parent.id()).map(c->householdId.equals(c.householdId())).orElse(false);}
    public record Result(int examined,int orphaned,int keptWithParent,int householdsUpdated,int failures,boolean truncated){}
}
