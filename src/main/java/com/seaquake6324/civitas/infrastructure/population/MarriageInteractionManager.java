package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.application.MarriageService;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.population.*;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.entity.CitizenEntity;
import com.seaquake6324.civitas.infrastructure.network.SubmitPlayerMarriageActionPayload;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Server-authoritative player/NPC proposal adapter; all world and persistence access stays on the main thread. */
public final class MarriageInteractionManager {
    private static final MarriageService SERVICE=new MarriageService();
    private static long attempts,accepted,rejected,stale,examinedEntities;

    public static String action(ServerPlayer player,CitizenRecord citizen){
        PopulationSavedData data=PopulationSavedData.get(player.level().getServer());
        if(data.readOnly())return "read_only";if(data.profile(player.getUUID()).isEmpty())return "no_profile";
        City city=CitySavedData.get(player.level().getServer()).cityForMember(player.getUUID()).orElse(null);
        if(city==null)return "not_member";if(!city.id().equals(citizen.cityId()))return "different_city";
        var npc=participant(data,citizen);var actor=player(data,city,player.getUUID());var result=MarriageEligibilityRules.evaluate(actor,npc,settings());
        if(!result.eligible())return result.failures().iterator().next().name().toLowerCase(Locale.ROOT);
        if(data.activeProposalFor(actor.member()).isPresent()||data.activeProposalFor(npc.member()).isPresent())return "active_proposal";if(player.level().getGameTime()<data.nextProposalAt(actor.member()))return "cooldown";
        return data.proposalCount()>=CivitasConfig.MARRIAGE_MAX_STORED_PROPOSALS.get()?"capacity":"available";
    }

    public static Result propose(ServerPlayer player,UUID citizenId,long expectedRevision){
        attempts++;if(!(player.level() instanceof ServerLevel level))return reject("invalid_world");PopulationSavedData data=PopulationSavedData.get(level.getServer());
        if(data.readOnly())return reject("read_only");if(data.profile(player.getUUID()).isEmpty())return reject("no_profile");CitizenRecord citizen=data.citizen(citizenId).orElse(null);
        if(citizen==null)return reject("missing");if(citizen.revision()!=expectedRevision){stale++;return reject("stale_revision");}
        boolean nearby=false;for(CitizenEntity entity:level.getEntitiesOfClass(CitizenEntity.class,player.getBoundingBox().inflate(8),e->e.citizenId().filter(citizenId::equals).isPresent())){examinedEntities++;nearby=true;break;}if(!nearby)return reject("not_nearby");
        City city=CitySavedData.get(level.getServer()).cityForMember(player.getUUID()).orElse(null);if(city==null||!city.id().equals(citizen.cityId()))return reject("different_city");
        var result=SERVICE.propose(data,UUID.randomUUID(),UUID.randomUUID(),player(data,city,player.getUUID()),participant(data,citizen),settings(),FamilyMemberRef.player(player.getUUID()),level.getGameTime(),level.getGameTime()+CivitasConfig.MARRIAGE_PROPOSAL_DURATION_TICKS.get(),CivitasConfig.MARRIAGE_MAX_STORED_PROPOSALS.get());
        if(!result.success())return reject(result.failure().name().toLowerCase(Locale.ROOT));accepted++;return new Result(true,"accepted");
    }

    public static PlayerView playerView(ServerPlayer actor,ServerPlayer target){
        PopulationSavedData data=PopulationSavedData.get(actor.level().getServer());FamilyMemberRef actorRef=FamilyMemberRef.player(actor.getUUID()),targetRef=FamilyMemberRef.player(target.getUUID());
        MarriageProposal any=data.activeProposalFor(actorRef).orElse(null);if(any!=null&&!any.involves(targetRef))return new PlayerView("active_proposal",any.id(),any.revision());MarriageProposal proposal=any;
        if(proposal!=null){boolean needsActor=proposal.proposer().equals(actorRef)?!proposal.proposerConfirmed():!proposal.targetConfirmed();return new PlayerView(needsActor?"accept":"waiting",proposal.id(),proposal.revision());}
        City city=CitySavedData.get(actor.level().getServer()).cityForMember(actor.getUUID()).orElse(null),other=CitySavedData.get(actor.level().getServer()).cityForMember(target.getUUID()).orElse(null);
        if(data.readOnly()||data.profile(actor.getUUID()).isEmpty()||data.profile(target.getUUID()).isEmpty()||city==null||other==null||!city.id().equals(other.id())||data.householdForPartner(actorRef).isPresent()||data.householdForPartner(targetRef).isPresent())return new PlayerView("unavailable",null,0);
        return new PlayerView("propose",null,0);
    }

    public static Result playerAction(ServerPlayer actor,UUID targetId,SubmitPlayerMarriageActionPayload.Action action,UUID proposalId,long expectedRevision){
        attempts++;if(!(actor.level() instanceof ServerLevel level))return reject("invalid_world");ServerPlayer target=level.getServer().getPlayerList().getPlayer(targetId);
        if(target==null||target.level()!=level||actor.distanceToSqr(target)>64)return reject("not_nearby");PopulationSavedData data=PopulationSavedData.get(level.getServer());
        if(data.readOnly())return reject("read_only");if(data.profile(actor.getUUID()).isEmpty()||data.profile(targetId).isEmpty())return reject("no_profile");
        City city=CitySavedData.get(level.getServer()).cityForMember(actor.getUUID()).orElse(null),other=CitySavedData.get(level.getServer()).cityForMember(targetId).orElse(null);if(city==null||other==null||!city.id().equals(other.id()))return reject("different_city");
        FamilyMemberRef actorRef=FamilyMemberRef.player(actor.getUUID());
        if(action==SubmitPlayerMarriageActionPayload.Action.PROPOSE){var result=SERVICE.propose(data,UUID.randomUUID(),UUID.randomUUID(),player(data,city,actor.getUUID()),player(data,city,targetId),settings(),actorRef,level.getGameTime(),level.getGameTime()+CivitasConfig.MARRIAGE_PROPOSAL_DURATION_TICKS.get(),CivitasConfig.MARRIAGE_MAX_STORED_PROPOSALS.get());if(!result.success())return reject(result.failure().name().toLowerCase(Locale.ROOT));return new Result(true,"pending");}
        if(proposalId==null)return reject("missing");MarriageProposal proposal=data.proposal(proposalId).orElse(null);if(proposal==null||!proposal.involves(actorRef)||!proposal.involves(FamilyMemberRef.player(targetId)))return reject("missing");
        MarriageService.ConfirmationResult result=action==SubmitPlayerMarriageActionPayload.Action.ACCEPT?SERVICE.confirm(data,proposalId,actorRef,expectedRevision,UUID.randomUUID(),level.getGameTime(),participantFor(data,city,proposal.proposer()),participantFor(data,city,proposal.target()),settings()):SERVICE.decline(data,proposalId,actorRef,expectedRevision,level.getGameTime());
        if(!result.success())return reject(result.failure().name().toLowerCase(Locale.ROOT));if(result.household()!=null){accepted++;return new Result(true,"accepted");}return new Result(true,"declined");
    }

    private static MarriageEligibilityRules.Participant player(PopulationSavedData data,City city,UUID id){FamilyMemberRef ref=FamilyMemberRef.player(id);long revision=data.profile(id).map(PlayerCivitasProfile::revision).orElse(0L);return new MarriageEligibilityRules.Participant(ref,city.id(),true,true,data.householdForPartner(ref).isPresent(),100,revision);}
    private static MarriageEligibilityRules.Participant participantFor(PopulationSavedData data,City city,FamilyMemberRef ref){return ref.kind()==FamilyMemberRef.Kind.PLAYER?player(data,city,ref.id()):participant(data,data.citizen(ref.id()).orElseThrow());}
    private static MarriageEligibilityRules.Participant participant(PopulationSavedData data,CitizenRecord citizen){AgeStage stage=PopulationAgingManager.rules().evaluate(citizen.ageTicks()).stage();boolean adult=stage!=AgeStage.CHILD&&stage!=AgeStage.ADOLESCENT;FamilyMemberRef ref=FamilyMemberRef.citizen(citizen.id());return new MarriageEligibilityRules.Participant(ref,citizen.cityId(),citizen.alive(),adult,data.householdForPartner(ref).isPresent()||citizen.householdId()!=null,citizen.settlementWillingness(),citizen.revision());}
    private static MarriageEligibilityRules.Settings settings(){return new MarriageEligibilityRules.Settings(CivitasConfig.MARRIAGE_NPC_WILLINGNESS_MINIMUM.get(),CivitasConfig.MARRIAGE_PROPOSAL_COOLDOWN_TICKS.get());}
    private static Result reject(String reason){rejected++;return new Result(false,reason);}
    public static Metrics metrics(){return new Metrics(attempts,accepted,rejected,stale,examinedEntities);}
    public record Result(boolean success,String reason){}public record PlayerView(String action,UUID proposalId,long proposalRevision){}public record Metrics(long attempts,long accepted,long rejected,long stale,long examinedEntities){}
    private MarriageInteractionManager(){}
}
