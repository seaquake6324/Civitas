package com.seaquake6324.civitas.application;

import com.seaquake6324.civitas.application.port.MigrationRepository;
import com.seaquake6324.civitas.domain.population.*;
import java.util.Map;
import java.util.UUID;

/** Atomic application boundary for converting settled NPCs into a real departing migration group. */
public final class StartOutMigrationService {
    public Result start(MigrationRepository repository,MigrationGroupRecord group,Household household,Map<UUID,Long>expectedRevisions,int cap){if(repository==null||group==null||expectedRevisions==null)return new Result(false,Failure.INVALID);if(group.state()!=MigrationGroupRecord.State.DEPARTING||!expectedRevisions.keySet().equals(group.members()))return new Result(false,Failure.INVALID);boolean committed=repository.beginOutMigration(group,household,expectedRevisions,cap);return new Result(committed,committed?null:Failure.STALE_OR_CONFLICT);}
    public enum Failure{INVALID,STALE_OR_CONFLICT}public record Result(boolean success,Failure failure){}
}
