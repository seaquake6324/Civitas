package com.seaquake6324.civitas.domain.population;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent world-migration application. Members exist physically and never move while invisible. */
public record MigrationGroupRecord(UUID id,UUID targetCityId,String dimension,long originPos,long targetPos,
        Set<UUID>members,Set<UUID>partners,Set<UUID>guardians,Set<UUID>children,Map<UUID,Long>memberPositions,
        State state,double attractionScore,long createdAt,long decisionDeadline,long revision) {
    public static final int HARD_MEMBER_CAP=8;
    public MigrationGroupRecord {
        if(id==null||targetCityId==null||dimension==null||dimension.isBlank()||state==null)throw new IllegalArgumentException("missing migration identity");
        members=Set.copyOf(members);partners=Set.copyOf(partners);guardians=Set.copyOf(guardians);children=Set.copyOf(children);memberPositions=Map.copyOf(memberPositions);
        if(members.isEmpty()||members.size()>HARD_MEMBER_CAP||partners.size()>2||guardians.size()>2||!members.containsAll(partners)||!members.containsAll(guardians)||!members.containsAll(children)||!java.util.Collections.disjoint(partners,children)||!java.util.Collections.disjoint(guardians,children)||!memberPositions.keySet().equals(members))throw new IllegalArgumentException("invalid migration family");
        if(!children.isEmpty()&&guardians.isEmpty())throw new IllegalArgumentException("migrant children require at least one source guardian");
        if(!Double.isFinite(attractionScore)||attractionScore<0||attractionScore>100||createdAt<0||decisionDeadline<createdAt||revision<0)throw new IllegalArgumentException("invalid migration state");
    }
    public MigrationGroupRecord(UUID id,UUID targetCityId,String dimension,long originPos,long targetPos,Set<UUID>members,Set<UUID>partners,Set<UUID>children,Map<UUID,Long>memberPositions,State state,double attractionScore,long createdAt,long decisionDeadline,long revision){this(id,targetCityId,dimension,originPos,targetPos,members,partners,children.isEmpty()?Set.of():partners,children,memberPositions,state,attractionScore,createdAt,decisionDeadline,revision);}
    public MigrationGroupRecord withPositions(Map<UUID,Long>positions){return new MigrationGroupRecord(id,targetCityId,dimension,originPos,targetPos,members,partners,guardians,children,positions,state,attractionScore,createdAt,decisionDeadline,revision+1);}
    public MigrationGroupRecord applied(long deadline){return new MigrationGroupRecord(id,targetCityId,dimension,originPos,targetPos,members,partners,guardians,children,memberPositions,State.APPLIED,attractionScore,createdAt,Math.max(createdAt,deadline),revision+1);}
    public MigrationGroupRecord departing(){return new MigrationGroupRecord(id,targetCityId,dimension,originPos,originPos,members,partners,guardians,children,memberPositions,State.DEPARTING,attractionScore,createdAt,decisionDeadline,revision+1);}
    public MigrationGroupRecord pooled(){return new MigrationGroupRecord(id,targetCityId,dimension,originPos,originPos,members,partners,guardians,children,memberPositions,State.REGIONAL_POOL,attractionScore,createdAt,decisionDeadline,revision+1);}
    public MigrationGroupRecord reactivate(UUID cityId,long newOrigin,long newTarget,long now,double attraction){Map<UUID,Long>positions=new java.util.HashMap<>();for(UUID member:members)positions.put(member,newOrigin);return new MigrationGroupRecord(id,cityId,dimension,newOrigin,newTarget,members,partners,guardians,children,positions,State.APPROACHING,Math.max(0,Math.min(100,attraction)),now,now,revision+1);}
    public enum State{APPROACHING,APPLIED,DEPARTING,REGIONAL_POOL}
}
