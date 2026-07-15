package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.application.CompleteBirthService;
import com.seaquake6324.civitas.application.ConceiveChildService;
import com.seaquake6324.civitas.application.PlanNpcReproductionPairsService;
import com.seaquake6324.civitas.domain.City;
import com.seaquake6324.civitas.domain.population.*;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import net.minecraft.server.MinecraftServer;

/** Main-thread bounded conception and due-birth orchestration. */
public final class ReproductionManager {
    private static final ReproductionRules RULES=new ReproductionRules();
    private static final ConceiveChildService CONCEPTION=new ConceiveChildService();
    private static final CompleteBirthService BIRTH=new CompleteBirthService();
    private static final PlanNpcReproductionPairsService PAIRING=new PlanNpcReproductionPairsService();
    private static MinecraftServer owner;private static long nextEvaluation;private static UUID citizenCursor;
    private static long tickRuns,householdRuns,householdsExamined,eligibleHouseholds,citizensExamined,nonSpousePairs,pairPlansTruncated,conceptions,conceptionFailures,failedRolls,cooldownSkips,playerConsentDeferred,pregnanciesExamined,births,carrierDeaths,birthFailures,totalMicros,maxMicros;
    private static String lastReason="not_run";

    public static void tick(MinecraftServer server){
        if(owner!=server)reset(server);
        PopulationSavedData population=PopulationSavedData.get(server);CitySavedData cities=CitySavedData.get(server);BuildingSavedData buildings=BuildingSavedData.get(server);CivilizationSavedData civilization=CivilizationSavedData.get(server);
        if(population.readOnly()||cities.readOnly()||buildings.readOnly()||civilization.readOnly()){lastReason="dependency_read_only";return;}
        long started=System.nanoTime(),now=server.overworld().getGameTime();
        ReproductionInteractionManager.maintain(population,now);
        processDue(population,cities,now);
        if(now>=nextEvaluation){nextEvaluation=now+CivitasConfig.REPRODUCTION_INTERVAL_TICKS.get();processHouseholds(population,cities,buildings,civilization,now);processNonSpousePairs(population,cities,now);}
        long micros=Math.max(0,(System.nanoTime()-started)/1_000);tickRuns++;totalMicros+=micros;maxMicros=Math.max(maxMicros,micros);
    }

    private static void processNonSpousePairs(PopulationSavedData population,CitySavedData cities,long now){
        var batch=population.citizenBatch(citizenCursor,CivitasConfig.REPRODUCTION_CITIZEN_SCAN_BATCH.get());AgeRules ages=PopulationAgingManager.rules();List<PlanNpcReproductionPairsService.Candidate>candidates=new ArrayList<>();
        for(CitizenRecord citizen:batch.records()){FamilyMemberRef ref=FamilyMemberRef.citizen(citizen.id());UUID cityId=citizen.cityId();boolean eligible=citizen.alive()&&RULES.fertility(ages.evaluate(citizen.ageTicks()).stage())>0&&population.nextReproductionAt(ref)<=now&&population.activePregnancy(ref).isEmpty()&&cityId!=null&&cities.byId(cityId).isPresent();candidates.add(new PlanNpcReproductionPairsService.Candidate(citizen.id(),cityId,citizen.gender(),eligible));}
        citizenCursor=batch.wrapped()?null:batch.nextCursor();var plan=PAIRING.plan(candidates,CivitasConfig.REPRODUCTION_PAIRS_PER_RUN.get());citizensExamined+=plan.examined();if(plan.truncated())pairPlansTruncated++;
        for(var pair:plan.pairs()){CitizenRecord first=population.citizen(pair.first().citizenId()).orElse(null),second=population.citizen(pair.second().citizenId()).orElse(null);if(first==null||second==null)continue;FamilyMemberRef aRef=FamilyMemberRef.citizen(first.id()),bRef=FamilyMemberRef.citizen(second.id());if(spouses(population,aRef).contains(bRef))continue;City city=cities.byId(first.cityId()).orElse(null);if(city==null||!first.cityId().equals(second.cityId()))continue;nonSpousePairs++;
            ReproductionRules.Participant a=participant(first,ages),b=participant(second,ages);ReproductionRules.Conditions conditions=ReproductionConditionResolver.resolve(owner,city,aRef,first,bRef,second,now).conditions();ReproductionRules.Weights weights=ReproductionSettings.weights();var evaluation=RULES.evaluate(a,b,conditions,weights);if(!evaluation.biologicallyEligible())continue;UUID pairId=derived(first.id(),"pair:"+second.id());if(roll(pairId,now)>=evaluation.effectiveWillingness()/100.0){long retry=now+evaluation.attemptIntervalTicks();population.setNextReproductionAt(aRef,retry);population.setNextReproductionAt(bRef,retry);failedRolls++;lastReason="non_spouse_roll_failed";continue;}
            var result=CONCEPTION.conceive(population,new ConceiveChildService.Request(derived(pairId,"pregnancy:"+now),city.id(),a,first.revision(),b,second.revision(),conditions,weights,spouses(population,aRef),spouses(population,bRef),now,CivitasConfig.PREGNANCY_DURATION_TICKS.get(),CivitasConfig.PREGNANCY_RECORD_CAP.get()));if(result.success()){conceptions++;lastReason=result.pregnancy().infidelity()?"non_spouse_conceived_infidelity":"non_spouse_conceived";}else{conceptionFailures++;lastReason="non_spouse_"+result.failure().name().toLowerCase(Locale.ROOT);}
        }
    }

    private static void processHouseholds(PopulationSavedData population,CitySavedData cities,BuildingSavedData buildings,CivilizationSavedData civilization,long now){
        var batch=population.householdBatch(population.reproductionCursor(),CivitasConfig.REPRODUCTION_HOUSEHOLDS_PER_RUN.get());
        if(batch.records().isEmpty()){lastReason="no_households";return;}
        for(Household household:batch.records()){
            householdsExamined++;
            City city=cities.byId(household.cityId()).orElse(null);if(household.partners().size()!=2||city==null){lastReason="not_partnered_or_city_missing";continue;}
            List<FamilyMemberRef>parents=stable(household.partners());
            if(parents.stream().anyMatch(p->p.kind()==FamilyMemberRef.Kind.PLAYER)){playerConsentDeferred++;lastReason="player_consent_required";continue;}
            CitizenRecord first=population.citizen(parents.get(0).id()).orElse(null),second=population.citizen(parents.get(1).id()).orElse(null);
            if(first==null||second==null||!first.alive()||!second.alive()){lastReason="missing_or_dead_parent";continue;}
            if(population.nextReproductionAt(parents.get(0))>now||population.nextReproductionAt(parents.get(1))>now){cooldownSkips++;lastReason="cooldown";continue;}
            AgeRules ageRules=PopulationAgingManager.rules();
            ReproductionRules.Participant a=participant(first,ageRules),b=participant(second,ageRules);ReproductionRules.Conditions conditions=ReproductionConditionResolver.resolve(owner,city,parents.get(0),first,parents.get(1),second,now).conditions();
            ReproductionRules.Weights weights=ReproductionSettings.weights();ReproductionRules.Result evaluation=RULES.evaluate(a,b,conditions,weights);
            if(!evaluation.biologicallyEligible()){lastReason=evaluation.reason();continue;}
            eligibleHouseholds++;
            if(roll(household.id(),now)>=evaluation.effectiveWillingness()/100.0){long retry=Math.addExact(now,evaluation.attemptIntervalTicks());population.setNextReproductionAt(parents.get(0),retry);population.setNextReproductionAt(parents.get(1),retry);failedRolls++;lastReason="conception_roll_failed";continue;}
            UUID pregnancyId=derived(household.id(),"pregnancy:"+now);
            var result=CONCEPTION.conceive(population,new ConceiveChildService.Request(pregnancyId,household.cityId(),a,first.revision(),b,second.revision(),conditions,weights,Set.of(parents.get(1)),Set.of(parents.get(0)),now,CivitasConfig.PREGNANCY_DURATION_TICKS.get(),CivitasConfig.PREGNANCY_RECORD_CAP.get()));
            if(result.success()){conceptions++;lastReason="conceived";}else{conceptionFailures++;lastReason="conception_"+result.failure().name().toLowerCase(Locale.ROOT);}
        }
        population.reproductionCursor(batch.wrapped()?null:batch.nextCursor());householdRuns++;
    }

    private static void processDue(PopulationSavedData population,CitySavedData cities,long now){
        var batch=population.pregnancyBatch(population.pregnancyCursor(),CivitasConfig.PREGNANCIES_PER_TICK.get());
        if(batch.records().isEmpty())return;
        for(PregnancyRecord pregnancy:batch.records()){
            pregnanciesExamined++;if(now<pregnancy.dueAt())continue;
            if(cities.byId(pregnancy.cityId()).isEmpty()){birthFailures++;lastReason="birth_city_missing";continue;}
            if(pregnancy.gestationalParent().kind()==FamilyMemberRef.Kind.CITIZEN){CitizenRecord carrier=population.citizen(pregnancy.gestationalParent().id()).orElse(null);if(carrier==null||!carrier.alive()){population.removePregnancy(pregnancy.id(),pregnancy.revision());carrierDeaths++;lastReason="carrier_died";continue;}}
            UUID childId=derived(pregnancy.id(),"child"),householdId=derived(pregnancy.id(),"guardian_household");
            var result=BIRTH.complete(population,new CompleteBirthService.Request(pregnancy.id(),pregnancy.revision(),childId,householdId,now,PopulationAgingManager.lifespanRules().years(childId),CivitasConfig.BIRTH_COOLDOWN_TICKS.get(),CivitasConfig.HOUSEHOLD_CHILD_SAFETY_CAP.get()));
            if(result.success()){births++;lastReason="birth_committed";}else{birthFailures++;lastReason="birth_"+result.failure().name().toLowerCase(Locale.ROOT);}
        }
        population.pregnancyCursor(batch.wrapped()?null:batch.nextCursor());
    }

    private static ReproductionRules.Participant participant(CitizenRecord citizen,AgeRules rules){double willingness=(citizen.settlementWillingness()+citizen.needs().social())/2.0;return new ReproductionRules.Participant(FamilyMemberRef.citizen(citizen.id()),citizen.gender(),rules.evaluate(citizen.ageTicks()).stage(),citizen.alive(),willingness);}
    private static List<FamilyMemberRef>stable(Collection<FamilyMemberRef>members){return members.stream().sorted(Comparator.comparing((FamilyMemberRef ref)->ref.kind().ordinal()).thenComparing(FamilyMemberRef::id)).toList();}
    private static Set<FamilyMemberRef>spouses(PopulationSavedData data,FamilyMemberRef ref){return data.householdForPartner(ref).map(h->{Set<FamilyMemberRef>out=new HashSet<>(h.partners());out.remove(ref);return Set.copyOf(out);}).orElse(Set.of());}
    private static double roll(UUID id,long now){long mixed=id.getMostSignificantBits()^Long.rotateLeft(id.getLeastSignificantBits(),19)^Long.rotateLeft(now,7);return new SplittableRandom(mixed).nextDouble();}
    private static UUID derived(UUID source,String suffix){return UUID.nameUUIDFromBytes((source+":"+suffix).getBytes(StandardCharsets.UTF_8));}
    private static void reset(MinecraftServer server){owner=server;nextEvaluation=0;citizenCursor=null;tickRuns=householdRuns=householdsExamined=eligibleHouseholds=citizensExamined=nonSpousePairs=pairPlansTruncated=conceptions=conceptionFailures=failedRolls=cooldownSkips=playerConsentDeferred=pregnanciesExamined=births=carrierDeaths=birthFailures=totalMicros=maxMicros=0;lastReason="server_reset";}
    public static Metrics metrics(){long runs=Math.max(1,tickRuns);return new Metrics(tickRuns,householdRuns,householdsExamined,eligibleHouseholds,citizensExamined,nonSpousePairs,pairPlansTruncated,conceptions,conceptionFailures,failedRolls,cooldownSkips,playerConsentDeferred,pregnanciesExamined,births,carrierDeaths,birthFailures,totalMicros/runs,maxMicros,lastReason);}
    public record Metrics(long tickRuns,long householdRuns,long householdsExamined,long eligibleHouseholds,long citizensExamined,long nonSpousePairs,long pairPlansTruncated,long conceptions,long conceptionFailures,long failedRolls,long cooldownSkips,long playerConsentDeferred,long pregnanciesExamined,long births,long carrierDeaths,long birthFailures,long averageMicros,long maxMicros,String lastReason){}
    private ReproductionManager(){}
}
