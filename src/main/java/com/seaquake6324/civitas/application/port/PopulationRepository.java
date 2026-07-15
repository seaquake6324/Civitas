package com.seaquake6324.civitas.application.port;

import com.seaquake6324.civitas.domain.population.CitizenRecord;
import com.seaquake6324.civitas.domain.population.Household;
import com.seaquake6324.civitas.domain.population.PlayerCivitasProfile;
import com.seaquake6324.civitas.domain.population.FamilyMemberRef;
import com.seaquake6324.civitas.domain.population.MarriageProposal;
import com.seaquake6324.civitas.domain.population.CitizenLocationRecord;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface PopulationRepository {
    Optional<PlayerCivitasProfile> profile(UUID playerId);
    void putProfile(PlayerCivitasProfile profile);
    Optional<CitizenRecord> citizen(UUID citizenId);
    Collection<CitizenRecord> citizens();
    void putCitizen(CitizenRecord citizen);
    default Optional<CitizenLocationRecord> location(UUID citizenId){return Optional.empty();}
    default boolean updateLocation(CitizenLocationRecord location,long expectedCitizenRevision,long expectedLocationRevision){return false;}
    Optional<Household> household(UUID householdId);
    Collection<Household> households();
    void putHousehold(Household household);
    boolean createHousehold(Household household,java.util.Map<UUID,Long> citizenExpectedRevisions);
    default boolean recordPartnerDeath(UUID householdId,FamilyMemberRef deceased,long expectedHouseholdRevision){return false;}
    Optional<Household> householdForPartner(FamilyMemberRef partner);
    Optional<MarriageProposal> proposal(UUID proposalId);
    Optional<MarriageProposal> activeProposalFor(FamilyMemberRef participant);
    default long nextProposalAt(FamilyMemberRef participant){return 0;}
    default void setNextProposalAt(FamilyMemberRef participant,long tick){}
    int proposalCount();
    void putProposal(MarriageProposal proposal);
}
