package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.MigrationRepository;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.population.MigrationGroupRecord;
import java.util.UUID;

public final class DecideMigrationService {
    public Result decide(MigrationRepository repository,City city,UUID actor,UUID groupId,long expectedRevision,boolean approve,UUID residenceId,long now){
        if(city==null||actor==null||groupId==null)return new Result(false,Failure.MISSING,null);
        if(!city.mayManage(actor))return new Result(false,Failure.NOT_MANAGER,null);
        MigrationGroupRecord group=repository.migration(groupId).orElse(null);if(group==null||!city.id().equals(group.targetCityId()))return new Result(false,Failure.MISSING,null);
        if(group.revision()!=expectedRevision)return new Result(false,Failure.STALE,group);
        if(group.state()!=MigrationGroupRecord.State.APPLIED)return new Result(false,Failure.NOT_APPLIED,group);
        if(approve)return repository.settleMigration(group,expectedRevision,residenceId,now)?new Result(true,null,group):new Result(false,Failure.STALE,group);
        MigrationGroupRecord departing=group.departing();return repository.updateMigration(departing,expectedRevision)?new Result(true,null,departing):new Result(false,Failure.STALE,group);
    }
    public enum Failure{MISSING,NOT_MANAGER,STALE,NOT_APPLIED}
    public record Result(boolean success,Failure failure,MigrationGroupRecord group){}
}
