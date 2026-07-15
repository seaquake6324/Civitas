package com.seaquake6324.civitas.application.port;

import com.seaquake6324.civitas.domain.population.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface MigrationRepository extends PopulationRepository {
    Optional<MigrationGroupRecord> migration(UUID id);
    Optional<MigrationGroupRecord> migrationForMember(UUID memberId);
    List<MigrationGroupRecord> migrationsForCity(UUID cityId,int limit);
    Collection<MigrationGroupRecord> migrations();
    int activeMigrationCount();
    boolean createMigration(MigrationGroupRecord group,Collection<CitizenRecord>members,int activeCap);
    boolean beginOutMigration(MigrationGroupRecord group,Household household,Map<UUID,Long>expectedCitizenRevisions,int activeCap);
    boolean updateMigration(MigrationGroupRecord group,long expectedRevision);
    boolean settleMigration(MigrationGroupRecord group,long expectedRevision,UUID residenceId,long settledAt);
    boolean poolDepartedMigration(MigrationGroupRecord group,long expectedRevision);
    Optional<MigrationOriginRecord> migrationOrigin(UUID citizenId);
}
