package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.application.ConceiveChildService;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.population.*;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.entity.CitizenEntity;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/** Main-thread adapter for explicit player reproduction consent and NPC willingness. */
public final class ReproductionInteractionManager {
    private static final ConceiveChildService SERVICE=new ConceiveChildService();
    private static final ReproductionRules RULES=new ReproductionRules();
    private static long attempts,conceived,rejected,examinedEntities,expiredConsents;

    public static String npcAction(ServerPlayer player,CitizenRecord citizen){
        PopulationSavedData data=PopulationSavedData.get(player.level().getServer());
        if(data.readOnly())return "read_only";PlayerCivitasProfile profile=data.profile(player.getUUID()).orElse(null);if(profile==null)return "no_profile";
        if(!nearby(player,citizen.id()))return "not_nearby";
        City city=CitySavedData.get(player.level().getServer()).byId(citizen.cityId()).orElse(null);if(city==null)return "city_missing";var result=RULES.evaluate(player(profile),npc(citizen),conditions(player.level().getServer(),city,FamilyMemberRef.player(player.getUUID()),null,FamilyMemberRef.citizen(citizen.id()),citizen,player.level().getGameTime()),ReproductionSettings.weights());
        if(!result.biologicallyEligible())return "ineligible";FamilyMemberRef carrier=profile.gender()==Gender.FEMALE?FamilyMemberRef.player(player.getUUID()):FamilyMemberRef.citizen(citizen.id());
        if(data.activePregnancy(carrier).isPresent())return "pregnant";if(data.nextReproductionAt(FamilyMemberRef.player(player.getUUID()))>player.level().getGameTime()||data.nextReproductionAt(FamilyMemberRef.citizen(citizen.id()))>player.level().getGameTime())return "cooldown";
        return data.pregnancyCount()>=CivitasConfig.PREGNANCY_RECORD_CAP.get()?"capacity":"available";
    }

    public static Result npcConceive(ServerPlayer player,UUID citizenId,long expectedRevision){attempts++;if(!(player.level() instanceof ServerLevel level))return reject("invalid_world");PopulationSavedData data=PopulationSavedData.get(level.getServer());CitizenRecord citizen=data.citizen(citizenId).orElse(null);PlayerCivitasProfile profile=data.profile(player.getUUID()).orElse(null);if(citizen==null||profile==null)return reject("missing");if(citizen.revision()!=expectedRevision)return reject("stale_revision");if(!nearby(player,citizenId))return reject("not_nearby");
        City city=CitySavedData.get(level.getServer()).byId(citizen.cityId()).orElse(null);if(city==null)return reject("city_missing");long now=level.getGameTime();ReproductionRules.Participant a=player(profile),b=npc(citizen);ReproductionRules.Conditions conditions=conditions(level.getServer(),city,a.member(),null,b.member(),citizen,now);ReproductionRules.Weights weights=ReproductionSettings.weights();var evaluation=RULES.evaluate(a,b,conditions,weights);if(!evaluation.biologicallyEligible())return reject("ineligible");
        if(data.nextReproductionAt(a.member())>now||data.nextReproductionAt(b.member())>now)return reject("cooldown");double roll=Math.floorMod(Objects.hash(player.getUUID(),citizenId,now/20),10_000)/100.0;if(roll>=evaluation.effectiveWillingness()){data.setNextReproductionAt(a.member(),now+evaluation.attemptIntervalTicks());data.setNextReproductionAt(b.member(),now+evaluation.attemptIntervalTicks());return reject("declined");}
        var result=SERVICE.conceive(data,new ConceiveChildService.Request(UUID.randomUUID(),citizen.cityId(),a,profile.revision(),b,citizen.revision(),conditions,weights,spouses(data,a.member()),spouses(data,b.member()),now,CivitasConfig.PREGNANCY_DURATION_TICKS.get(),CivitasConfig.PREGNANCY_RECORD_CAP.get()));if(!result.success())return reject(result.failure()==ConceiveChildService.Failure.INELIGIBLE?"ineligible":"stale_or_capacity");conceived++;return new Result(true,"conceived",null,0);
    }

    public static PlayerView playerView(ServerPlayer actor,ServerPlayer target){PopulationSavedData data=PopulationSavedData.get(actor.level().getServer());expire(data,actor.level().getGameTime());FamilyMemberRef a=FamilyMemberRef.player(actor.getUUID()),b=FamilyMemberRef.player(target.getUUID());ReproductionConsent consent=data.activeReproductionConsent(a).orElse(null);if(consent!=null&&!consent.involves(b))return new PlayerView("active_request",consent.id(),consent.revision());if(consent!=null)return new PlayerView(consent.proposer().equals(a)?"waiting":"accept",consent.id(),consent.revision());if(!eligiblePlayers(actor,target,data))return new PlayerView("unavailable",null,0);return new PlayerView("propose",null,0);}

    public static Result playerAction(ServerPlayer actor,UUID targetId,Action action,UUID consentId,long expectedRevision){attempts++;if(!(actor.level() instanceof ServerLevel level))return reject("invalid_world");ServerPlayer target=level.getServer().getPlayerList().getPlayer(targetId);if(target==null||target.level()!=level||actor.distanceToSqr(target)>64)return reject("not_nearby");PopulationSavedData data=PopulationSavedData.get(level.getServer());expire(data,level.getGameTime());if(!eligiblePlayers(actor,target,data))return reject("unavailable");FamilyMemberRef a=FamilyMemberRef.player(actor.getUUID()),b=FamilyMemberRef.player(targetId);City city=CitySavedData.get(level.getServer()).cityForMember(actor.getUUID()).orElseThrow();long now=level.getGameTime();
        if(action==Action.PROPOSE){ReproductionConsent consent=new ReproductionConsent(UUID.randomUUID(),city.id(),a,b,now,now+CivitasConfig.REPRODUCTION_CONSENT_DURATION_TICKS.get(),1);return data.createReproductionConsent(consent,CivitasConfig.REPRODUCTION_CONSENT_CAP.get())?new Result(true,"pending",consent.id(),consent.revision()):reject("active_request");}
        ReproductionConsent consent=consentId==null?null:data.reproductionConsent(consentId).orElse(null);if(consent==null||consent.revision()!=expectedRevision||!consent.proposer().equals(b)||!consent.target().equals(a))return reject("stale_request");if(action==Action.DECLINE){data.removeReproductionConsent(consent.id(),consent.revision());return new Result(true,"declined",null,0);}
        PlayerCivitasProfile ap=data.profile(actor.getUUID()).orElseThrow(),tp=data.profile(targetId).orElseThrow();ReproductionRules.Participant first=player(tp),second=player(ap);ReproductionRules.Conditions conditions=conditions(level.getServer(),city,b,null,a,null,now);var result=SERVICE.conceive(data,new ConceiveChildService.Request(UUID.randomUUID(),city.id(),first,tp.revision(),second,ap.revision(),conditions,ReproductionSettings.weights(),spouses(data,b),spouses(data,a),now,CivitasConfig.PREGNANCY_DURATION_TICKS.get(),CivitasConfig.PREGNANCY_RECORD_CAP.get()));if(!result.success())return reject("stale_or_capacity");data.removeReproductionConsent(consent.id(),consent.revision());conceived++;return new Result(true,"conceived",null,0);
    }

    private static boolean eligiblePlayers(ServerPlayer a,ServerPlayer b,PopulationSavedData data){if(data.readOnly()||a.distanceToSqr(b)>64)return false;PlayerCivitasProfile ap=data.profile(a.getUUID()).orElse(null),bp=data.profile(b.getUUID()).orElse(null);if(ap==null||bp==null||ap.gender()==bp.gender())return false;City ac=CitySavedData.get(a.level().getServer()).cityForMember(a.getUUID()).orElse(null),bc=CitySavedData.get(a.level().getServer()).cityForMember(b.getUUID()).orElse(null);if(ac==null||bc==null||!ac.id().equals(bc.id()))return false;FamilyMemberRef carrier=ap.gender()==Gender.FEMALE?FamilyMemberRef.player(a.getUUID()):FamilyMemberRef.player(b.getUUID());long now=a.level().getGameTime();return data.activePregnancy(carrier).isEmpty()&&data.nextReproductionAt(FamilyMemberRef.player(a.getUUID()))<=now&&data.nextReproductionAt(FamilyMemberRef.player(b.getUUID()))<=now;}
    private static boolean nearby(ServerPlayer player,UUID citizenId){if(!(player.level() instanceof ServerLevel level))return false;for(CitizenEntity ignored:level.getEntitiesOfClass(CitizenEntity.class,player.getBoundingBox().inflate(8),e->e.citizenId().filter(citizenId::equals).isPresent())){examinedEntities++;return true;}return false;}
    private static ReproductionRules.Participant player(PlayerCivitasProfile p){return new ReproductionRules.Participant(FamilyMemberRef.player(p.playerId()),p.gender(),AgeStage.YOUNG_ADULT,true,100);}
    private static ReproductionRules.Participant npc(CitizenRecord c){return new ReproductionRules.Participant(FamilyMemberRef.citizen(c.id()),c.gender(),PopulationAgingManager.rules().evaluate(c.ageTicks()).stage(),c.alive(),c.settlementWillingness());}
    private static ReproductionRules.Conditions conditions(net.minecraft.server.MinecraftServer server,City city,FamilyMemberRef a,CitizenRecord ac,FamilyMemberRef b,CitizenRecord bc,long now){return ReproductionConditionResolver.resolve(server,city,a,ac,b,bc,now).conditions();}
    private static Set<FamilyMemberRef> spouses(PopulationSavedData data,FamilyMemberRef ref){return data.householdForPartner(ref).map(h->{Set<FamilyMemberRef>s=new HashSet<>(h.partners());s.remove(ref);return Set.copyOf(s);}).orElse(Set.of());}
    public static void maintain(PopulationSavedData data,long now){int examined=0;for(ReproductionConsent c:List.copyOf(data.reproductionConsents())){if(examined++>=64)break;if(c.expiresAt()<=now&&data.removeReproductionConsent(c.id(),c.revision()))expiredConsents++;}}
    private static void expire(PopulationSavedData data,long now){maintain(data,now);}
    private static Result reject(String reason){rejected++;return new Result(false,reason,null,0);}
    public static Metrics metrics(){return new Metrics(attempts,conceived,rejected,examinedEntities,expiredConsents);}
    public enum Action{PROPOSE,ACCEPT,DECLINE}
    public record Result(boolean success,String reason,UUID consentId,long consentRevision){}
    public record PlayerView(String action,UUID consentId,long consentRevision){}
    public record Metrics(long attempts,long conceived,long rejected,long examinedEntities,long expiredConsents){}
    private ReproductionInteractionManager(){}
}
