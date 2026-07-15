package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.application.ApplyCitizenIntentService;
import com.seaquake6324.civitas.application.PlanCitizenSimulationService;
import com.seaquake6324.civitas.application.RecordPartnerDeathService;
import com.seaquake6324.civitas.application.ReconcileOrphansAfterParentDeathService;
import com.seaquake6324.civitas.domain.population.AgeRules;
import com.seaquake6324.civitas.domain.population.CitizenIntent;
import com.seaquake6324.civitas.domain.population.CitizenRecord;
import com.seaquake6324.civitas.domain.population.CitizenSimulationSnapshot;
import com.seaquake6324.civitas.domain.population.FamilyMemberRef;
import com.seaquake6324.civitas.domain.population.LifespanRules;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.BuildingSavedData;
import com.seaquake6324.civitas.infrastructure.persistence.CitySavedData;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAccumulator;
import java.util.concurrent.atomic.LongAdder;
import net.minecraft.server.MinecraftServer;

/** Bounded snapshot -> pure worker -> revision-checked server-thread intent pipeline. */
public final class PopulationAgingManager {
    private static final PlanCitizenSimulationService PLANNER=new PlanCitizenSimulationService();
    private static final ApplyCitizenIntentService APPLIER=new ApplyCitizenIntentService();
    private static final RecordPartnerDeathService PARTNER_DEATH=new RecordPartnerDeathService();
    private static final ReconcileOrphansAfterParentDeathService ORPHANS=new ReconcileOrphansAfterParentDeathService();
    private static MinecraftServer owner;
    private static ThreadPoolExecutor executor;
    private static ArrayBlockingQueue<CitizenIntent> results;
    private static Set<UUID> inFlight=ConcurrentHashMap.newKeySet();
    private static Counters counters=new Counters();
    private static long nextRun;
    private static int configuredThreads,configuredWorkCapacity,configuredResultCapacity;
    private static boolean configAdjusted;

    public static void tick(MinecraftServer server){
        ensure(server);
        PopulationSavedData population=PopulationSavedData.get(server);
        CitySavedData cities=CitySavedData.get(server);BuildingSavedData buildings=BuildingSavedData.get(server);
        if(population.readOnly()||cities.readOnly()||buildings.readOnly()){discardPending(population.readOnly()?"population_read_only":cities.readOnly()?"city_read_only":"building_read_only");return;}
        long now=server.overworld().getGameTime();
        applyResults(server,population,now);
        if(now<nextRun)return;
        nextRun=now+CivitasConfig.POPULATION_AGING_INTERVAL_TICKS.get();
        long started=System.nanoTime();
        var batch=population.citizenBatch(population.simulationCursor(),CivitasConfig.POPULATION_AGING_BATCH_SIZE.get());
        if(batch.records().isEmpty())return;
        AgeRules ageRules=rules();LifespanRules lifespanRules=lifespanRules();
        for(CitizenRecord citizen:batch.records()){
            counters.snapshots.increment();
            if(!citizen.alive())continue;
            var city=citizen.cityId()==null?null:cities.byId(citizen.cityId()).orElse(null);
            if(city==null){counters.missingDependencies.increment();counters.lastReason="missing_city";continue;}
            ArrayList<CitizenSimulationSnapshot.BuildingRevision> dependencies=new ArrayList<>(2);
            HashSet<UUID> seen=new HashSet<>();
            addBuilding(dependencies,seen,buildings,citizen.residenceId());
            addBuilding(dependencies,seen,buildings,citizen.workBuildingId());
            CitizenSimulationSnapshot snapshot=new CitizenSimulationSnapshot(citizen,city.id(),city.revision(),dependencies,now,ageRules,lifespanRules);
            submit(snapshot);
        }
        population.simulationCursor(batch.wrapped()?null:batch.nextCursor());
        counters.batches.increment();counters.snapshotMicros.add(Math.max(0,(System.nanoTime()-started)/1_000));
    }

    private static void addBuilding(ArrayList<CitizenSimulationSnapshot.BuildingRevision> output,Set<UUID>seen,
            BuildingSavedData buildings,UUID id){if(id==null||!seen.add(id))return;var record=buildings.byId(id).orElse(null);if(record==null){output.add(CitizenSimulationSnapshot.BuildingRevision.missing(id));counters.missingDependencies.increment();}else output.add(new CitizenSimulationSnapshot.BuildingRevision(id,record.revision()));}

    private static boolean submit(CitizenSimulationSnapshot snapshot){
        if(!inFlight.add(snapshot.citizen().id())){counters.duplicateInFlight.increment();return false;}
        var targetResults=results;var targetInFlight=inFlight;var targetCounters=counters;
        try{
            executor.execute(()->{
                long started=System.nanoTime();
                boolean retained=false;
                try{
                    var planned=PLANNER.plan(snapshot);
                    if(planned.isEmpty()){targetInFlight.remove(snapshot.citizen().id());return;}
                    if(!targetResults.offer(planned.orElseThrow())){targetCounters.resultDropped.increment();targetCounters.lastReason="result_queue_full";targetInFlight.remove(snapshot.citizen().id());}
                    else{retained=true;targetCounters.workerCompleted.increment();}
                }catch(RuntimeException exception){targetCounters.workerFailures.increment();targetCounters.lastReason="worker_failure:"+exception.getClass().getSimpleName();}
                catch(Error error){targetCounters.workerFailures.increment();targetCounters.lastReason="worker_error:"+error.getClass().getSimpleName();throw error;}
                finally{if(!retained)targetInFlight.remove(snapshot.citizen().id());long micros=Math.max(0,(System.nanoTime()-started)/1_000);targetCounters.workerRuns.increment();targetCounters.workerMicros.add(micros);targetCounters.maxWorkerMicros.accumulate(micros);}
            });
            counters.submitted.increment();
            return true;
        }catch(RejectedExecutionException exception){inFlight.remove(snapshot.citizen().id());counters.taskRejected.increment();counters.lastReason="work_queue_full";return false;}
    }

    private static void applyResults(MinecraftServer server,PopulationSavedData population,long now){
        int cap=CivitasConfig.POPULATION_INTENTS_APPLY_PER_TICK.get();
        CitySavedData cities=CitySavedData.get(server);BuildingSavedData buildings=BuildingSavedData.get(server);
        long started=System.nanoTime();int examined=0;
        while(examined<cap){CitizenIntent intent=results.poll();if(intent==null)break;examined++;
            try{
                if(now-intent.snapshotAt()>CivitasConfig.POPULATION_INTENT_MAX_AGE_TICKS.get()){counters.expiredResults.increment();counters.lastReason="expired_result";continue;}
                if(!intent.ageRules().equals(rules())||!intent.lifespanRules().equals(lifespanRules())){counters.staleResults.increment();counters.lastReason="rules_changed";continue;}
                var applied=APPLIER.apply(population,cities,buildings,intent);
                if(!applied.success()){counters.staleResults.increment();counters.lastReason=applied.failure().name().toLowerCase(java.util.Locale.ROOT);continue;}
                counters.applied.increment();
                if(intent.beforeStage()!=intent.afterStage())counters.stageChanges.increment();
                if(intent.permanentDeath()){counters.deaths.increment();CitizenMaterializationManager.discardPermanentDeath(server,intent.citizenId());var history=PARTNER_DEATH.record(population,applied.citizen());if(!history.success()){counters.partnerDeathFailures.increment();counters.lastReason="partner_death_"+history.failure().name().toLowerCase(java.util.Locale.ROOT);}var orphanResult=ORPHANS.reconcile(population,FamilyMemberRef.citizen(intent.citizenId()),now,rules(),CivitasConfig.PARENTAGE_PER_DEATH.get());counters.orphansCreated.add(orphanResult.orphaned());counters.orphanFailures.add(orphanResult.failures());if(orphanResult.truncated())counters.orphanTruncations.increment();}
                counters.lastReason="applied";
            }finally{inFlight.remove(intent.citizenId());}
        }
        if(examined>0){long micros=Math.max(0,(System.nanoTime()-started)/1_000);counters.applyBatches.increment();counters.applyMicros.add(micros);counters.maxApplyMicros.accumulate(micros);}
    }

    private static void ensure(MinecraftServer server){
        int threads=CivitasConfig.POPULATION_WORKER_THREADS.get(),work=CivitasConfig.POPULATION_WORK_QUEUE_CAP.get(),result=CivitasConfig.POPULATION_INTENT_QUEUE_CAP.get();
        if(owner==server&&executor!=null&&threads==configuredThreads&&work==configuredWorkCapacity&&result==configuredResultCapacity)return;
        reset(server,threads,work,result);
    }

    private static void reset(MinecraftServer server,int threads,int workCapacity,int resultCapacity){
        if(executor!=null)executor.shutdownNow();
        owner=server;configuredThreads=threads;configuredWorkCapacity=workCapacity;configuredResultCapacity=resultCapacity;
        results=new ArrayBlockingQueue<>(resultCapacity);inFlight=ConcurrentHashMap.newKeySet();counters=new Counters();nextRun=0;
        AtomicInteger sequence=new AtomicInteger();
        ThreadFactory factory=task->{Thread thread=new Thread(task,"Civitas-Population-"+sequence.incrementAndGet());thread.setDaemon(true);thread.setUncaughtExceptionHandler((ignored,error)->{});return thread;};
        executor=new ThreadPoolExecutor(threads,threads,0,TimeUnit.MILLISECONDS,new ArrayBlockingQueue<>(workCapacity),factory,new ThreadPoolExecutor.AbortPolicy());
        counters.lastReason="server_reset";
    }

    private static void discardPending(String reason){CitizenIntent intent;while((intent=results.poll())!=null)inFlight.remove(intent.citizenId());counters.lastReason=reason;}

    public static void shutdown(MinecraftServer server){if(owner!=server)return;if(executor!=null)executor.shutdownNow();if(results!=null)results.clear();inFlight.clear();owner=null;executor=null;counters.lastReason="server_stopped";}

    static void initializeWorkerForTest(int threads,int workCapacity,int resultCapacity){reset(null,threads,workCapacity,resultCapacity);}
    static boolean submitSnapshotForTest(CitizenSimulationSnapshot snapshot){return submit(snapshot);}
    static void shutdownWorkerForTest(){shutdown(null);}

    public static AgeRules rules(){int adolescent=CivitasConfig.POPULATION_ADOLESCENT_AT.get();int young=Math.max(adolescent+1,CivitasConfig.POPULATION_YOUNG_ADULT_AT.get());int mature=Math.max(young+1,CivitasConfig.POPULATION_MATURE_ADULT_AT.get());int elder=Math.max(mature+1,CivitasConfig.POPULATION_ELDER_AT.get());configAdjusted=young!=CivitasConfig.POPULATION_YOUNG_ADULT_AT.get()||mature!=CivitasConfig.POPULATION_MATURE_ADULT_AT.get()||elder!=CivitasConfig.POPULATION_ELDER_AT.get();return new AgeRules(CivitasConfig.POPULATION_TICKS_PER_YEAR.get(),adolescent,young,mature,elder);}
    public static LifespanRules lifespanRules(){int minimum=CivitasConfig.POPULATION_MIN_LIFESPAN_YEARS.get(),maximum=Math.max(minimum,CivitasConfig.POPULATION_MAX_LIFESPAN_YEARS.get());configAdjusted|=maximum!=CivitasConfig.POPULATION_MAX_LIFESPAN_YEARS.get();return new LifespanRules(minimum,maximum);}

    public static Metrics metrics(){Counters c=counters;long batches=c.batches.sum(),applyBatches=c.applyBatches.sum(),workerRuns=c.workerRuns.sum();return new Metrics(c.applied.sum(),c.stageChanges.sum(),c.deaths.sum(),batches,c.snapshotMicros.sum(),batches==0?0:c.snapshotMicros.sum()/batches,configAdjusted,c.snapshots.sum(),c.submitted.sum(),c.workerCompleted.sum(),c.taskRejected.sum(),c.resultDropped.sum(),c.workerFailures.sum(),c.duplicateInFlight.sum(),c.staleResults.sum(),c.expiredResults.sum(),c.missingDependencies.sum(),c.partnerDeathFailures.sum(),c.orphansCreated.sum(),c.orphanFailures.sum(),c.orphanTruncations.sum(),executor==null?0:executor.getQueue().size(),results==null?0:results.size(),inFlight.size(),configuredThreads,workerRuns==0?0:c.workerMicros.sum()/workerRuns,c.maxWorkerMicros.get(),applyBatches==0?0:c.applyMicros.sum()/applyBatches,c.maxApplyMicros.get(),c.lastReason);}
    public record Metrics(long processed,long stageChanges,long deaths,long batches,long totalMicros,long averageBatchMicros,boolean configAdjusted,long snapshots,long submitted,long workerCompleted,long taskRejected,long resultDropped,long workerFailures,long duplicateInFlight,long staleResults,long expiredResults,long missingDependencies,long partnerDeathFailures,long orphansCreated,long orphanFailures,long orphanTruncations,int workQueue,int intentQueue,int inFlight,int workerThreads,long averageWorkerMicros,long maxWorkerMicros,long averageApplyMicros,long maxApplyMicros,String lastReason){}
    private static final class Counters{final LongAdder snapshots=new LongAdder(),submitted=new LongAdder(),workerRuns=new LongAdder(),workerCompleted=new LongAdder(),taskRejected=new LongAdder(),resultDropped=new LongAdder(),workerFailures=new LongAdder(),duplicateInFlight=new LongAdder(),applied=new LongAdder(),stageChanges=new LongAdder(),deaths=new LongAdder(),batches=new LongAdder(),snapshotMicros=new LongAdder(),staleResults=new LongAdder(),expiredResults=new LongAdder(),missingDependencies=new LongAdder(),partnerDeathFailures=new LongAdder(),orphansCreated=new LongAdder(),orphanFailures=new LongAdder(),orphanTruncations=new LongAdder(),workerMicros=new LongAdder(),applyBatches=new LongAdder(),applyMicros=new LongAdder();final LongAccumulator maxWorkerMicros=new LongAccumulator(Long::max,0),maxApplyMicros=new LongAccumulator(Long::max,0);volatile String lastReason="not_run";}
    private PopulationAgingManager(){}
}
