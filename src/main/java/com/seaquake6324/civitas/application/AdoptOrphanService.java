package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.ReproductionRepository;
import com.seaquake6324.civitas.domain.population.*;
import java.util.Map;
import java.util.UUID;

/** Revision-checked application commit; it never creates a child. */
public final class AdoptOrphanService {
    public Result adopt(ReproductionRepository repository,Request request){
        OrphanRecord orphan=repository.orphan(request.childId()).orElse(null);CitizenRecord child=repository.citizen(request.childId()).orElse(null);if(orphan==null||child==null)return new Result(false,Failure.MISSING);
        if(orphan.revision()!=request.orphanRevision()||child.revision()!=request.childRevision())return new Result(false,Failure.STALE);
        Household origin=child.householdId()==null?null:repository.household(child.householdId()).orElse(null);if(origin!=null&&origin.revision()!=request.originRevision())return new Result(false,Failure.STALE);
        Household adoptive=request.adoptive();CitizenRecord moved=child.withHousehold(adoptive.id());if(adoptive.residenceId()!=null&&!java.util.Objects.equals(moved.residenceId(),adoptive.residenceId()))moved=moved.withResidence(adoptive.residenceId());Household originAfter=origin==null?null:origin.removeChild(child.id());
        boolean committed=repository.commitAdoption(orphan,child.revision(),adoptive,request.adoptiveExpectedRevision(),origin,originAfter,moved,request.guardianRevisions(),request.confirmedPlayers(),request.childCapacity());return new Result(committed,committed?null:Failure.STALE_OR_CAPACITY);
    }
    public record Request(UUID childId,long orphanRevision,long childRevision,Household adoptive,long adoptiveExpectedRevision,long originRevision,Map<UUID,Long>guardianRevisions,java.util.Set<UUID>confirmedPlayers,int childCapacity){
        public Request{guardianRevisions=Map.copyOf(guardianRevisions);confirmedPlayers=java.util.Set.copyOf(confirmedPlayers);if(childId==null||adoptive==null||childCapacity<1)throw new IllegalArgumentException("invalid adoption request");}
        public Request(UUID childId,long orphanRevision,long childRevision,Household adoptive,long adoptiveExpectedRevision,long originRevision,Map<UUID,Long>guardianRevisions,int childCapacity){this(childId,orphanRevision,childRevision,adoptive,adoptiveExpectedRevision,originRevision,guardianRevisions,java.util.Set.of(),childCapacity);}
    }
    public enum Failure{MISSING,STALE,STALE_OR_CAPACITY}
    public record Result(boolean success,Failure failure){}
}
