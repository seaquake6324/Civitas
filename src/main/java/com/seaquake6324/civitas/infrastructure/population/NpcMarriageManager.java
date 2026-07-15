package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.application.MarriageService;
import com.seaquake6324.civitas.application.PlanNpcMarriagesService;
import com.seaquake6324.civitas.domain.population.*;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import java.util.*;
import net.minecraft.server.MinecraftServer;

/** Main-thread adapter for neutral, order-stable, bounded NPC matching and final revision revalidation. */
public final class NpcMarriageManager {
    private static final MarriageService MARRIAGE=new MarriageService();private static final PlanNpcMarriagesService PLANNER=new PlanNpcMarriagesService();private static MinecraftServer owner;private static long nextRun,runs,examined,eligible,formed,rejected,stale,totalMicros,maxMicros;
    public static void tick(MinecraftServer server){if(owner!=server)reset(server);long now=server.overworld().getGameTime();if(now<nextRun)return;nextRun=now+CivitasConfig.NPC_MARRIAGE_INTERVAL_TICKS.get();long started=System.nanoTime();PopulationSavedData data=PopulationSavedData.get(server);var batch=data.citizenBatch(data.marriageMatchingCursor(),CivitasConfig.NPC_MARRIAGE_SCAN_BATCH.get());List<PlanNpcMarriagesService.Candidate>snapshots=batch.records().stream().map(c->snapshot(data,c)).toList();var plan=PLANNER.plan(snapshots,CivitasConfig.MARRIAGE_NPC_WILLINGNESS_MINIMUM.get(),CivitasConfig.NPC_MARRIAGE_PAIRS_PER_RUN.get());examined+=plan.examined();eligible+=plan.eligible();for(var pair:plan.pairs()){CitizenRecord first=data.citizen(pair.first().member().id()).orElse(null),second=data.citizen(pair.second().member().id()).orElse(null);if(first==null||second==null||first.revision()!=pair.first().revision()||second.revision()!=pair.second().revision()){stale++;continue;}var result=MARRIAGE.propose(data,UUID.randomUUID(),UUID.randomUUID(),participant(data,first),participant(data,second),settings(),null,now,now+CivitasConfig.MARRIAGE_PROPOSAL_DURATION_TICKS.get(),CivitasConfig.MARRIAGE_MAX_STORED_PROPOSALS.get());if(result.success())formed++;else rejected++;}UUID cursor=plan.examined()==0?data.marriageMatchingCursor():batch.records().get(plan.examined()-1).id();boolean consumedAll=plan.examined()==batch.records().size();data.marriageMatchingCursor(consumedAll&&batch.wrapped()?null:cursor);runs++;long micros=Math.max(0,(System.nanoTime()-started)/1_000);totalMicros+=micros;maxMicros=Math.max(maxMicros,micros);}
    private static PlanNpcMarriagesService.Candidate snapshot(PopulationSavedData data,CitizenRecord c){AgeStage stage=PopulationAgingManager.rules().evaluate(c.ageTicks()).stage();FamilyMemberRef ref=FamilyMemberRef.citizen(c.id());return new PlanNpcMarriagesService.Candidate(ref,c.cityId(),c.alive(),stage!=AgeStage.CHILD&&stage!=AgeStage.ADOLESCENT,data.householdForPartner(ref).isPresent()||c.householdId()!=null,data.activeProposalFor(ref).isPresent(),c.settlementWillingness(),c.revision());}
    private static MarriageEligibilityRules.Participant participant(PopulationSavedData data,CitizenRecord c){FamilyMemberRef ref=FamilyMemberRef.citizen(c.id());AgeStage stage=PopulationAgingManager.rules().evaluate(c.ageTicks()).stage();return new MarriageEligibilityRules.Participant(ref,c.cityId(),c.alive(),stage!=AgeStage.CHILD&&stage!=AgeStage.ADOLESCENT,data.householdForPartner(ref).isPresent()||c.householdId()!=null,c.settlementWillingness(),c.revision());}
    private static MarriageEligibilityRules.Settings settings(){return new MarriageEligibilityRules.Settings(CivitasConfig.MARRIAGE_NPC_WILLINGNESS_MINIMUM.get(),CivitasConfig.MARRIAGE_PROPOSAL_COOLDOWN_TICKS.get());}
    private static void reset(MinecraftServer server){owner=server;nextRun=runs=examined=eligible=formed=rejected=stale=totalMicros=maxMicros=0;}
    public static Metrics metrics(){return new Metrics(runs,examined,eligible,formed,rejected,stale,totalMicros,runs==0?0:totalMicros/runs,maxMicros);}
    public record Metrics(long runs,long examined,long eligible,long formed,long rejected,long stale,long totalMicros,long averageMicros,long maxMicros){}
    private NpcMarriageManager(){}
}
