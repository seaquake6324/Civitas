package com.seaquake6324.civitas.application.port;

import com.seaquake6324.civitas.domain.population.*;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Atomic persistence boundary for pregnancies, births and real orphan records. */
public interface ReproductionRepository extends PopulationRepository {
    Optional<PregnancyRecord> pregnancy(UUID id);
    Optional<PregnancyRecord> activePregnancy(FamilyMemberRef carrier);
    default Optional<Household> householdForGuardian(FamilyMemberRef guardian){return Optional.empty();}
    Collection<PregnancyRecord> pregnancies();
    Collection<ParentageRecord> parentageForParent(FamilyMemberRef parent,int limit);
    Optional<ParentageRecord> parentage(UUID childId);
    Optional<OrphanRecord> orphan(UUID childId);
    void putOrphan(OrphanRecord orphan);
    boolean commitAdoption(OrphanRecord orphan,long expectedChildRevision,Household adoptive,long expectedAdoptiveRevision,
                           Household origin,Household originAfter,CitizenRecord childAfter,Map<UUID,Long>guardianRevisions,
                           java.util.Set<UUID>confirmedPlayers,int childCapacity);
    int pregnancyCount();
    long nextReproductionAt(FamilyMemberRef member);
    void setNextReproductionAt(FamilyMemberRef member,long tick);
    boolean createPregnancy(PregnancyRecord pregnancy, Map<FamilyMemberRef,Long> expectedRevisions,
                            int pregnancyCapacity, long now);
    boolean commitBirth(PregnancyRecord pregnancy, CitizenRecord child, ParentageRecord parentage,
                        Household household, Map<FamilyMemberRef,Long> expectedParentRevisions,
                        long cooldownUntil, int householdChildCapacity);
}
