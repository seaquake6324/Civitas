package com.seaquake6324.civitas.domain.population;

import java.util.UUID;

/** Compact permanent provenance retained after an applicant becomes a citizen. */
public record MigrationOriginRecord(UUID citizenId,UUID groupId,UUID cityId,String dimension,long originPos,long settledAt,long revision){
    public MigrationOriginRecord{if(citizenId==null||groupId==null||cityId==null||dimension==null||dimension.isBlank()||settledAt<0||revision<0)throw new IllegalArgumentException("invalid migration origin");}
}
