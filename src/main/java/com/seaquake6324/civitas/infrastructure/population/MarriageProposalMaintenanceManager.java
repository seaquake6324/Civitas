package com.seaquake6324.civitas.infrastructure.population;

import com.seaquake6324.civitas.domain.population.MarriageProposal;
import com.seaquake6324.civitas.infrastructure.config.CivitasConfig;
import com.seaquake6324.civitas.infrastructure.persistence.PopulationSavedData;
import net.minecraft.server.MinecraftServer;

/** Main-thread, cursor-bounded proposal expiry and terminal-history pruning. */
public final class MarriageProposalMaintenanceManager {
    private static MinecraftServer owner;private static long nextRun,batches,examined,expired,purged,totalMicros,maxBatchMicros;
    public static void tick(MinecraftServer server){if(owner!=server){owner=server;nextRun=batches=examined=expired=purged=totalMicros=maxBatchMicros=0;}long now=server.overworld().getGameTime();if(now<nextRun)return;nextRun=now+CivitasConfig.MARRIAGE_MAINTENANCE_INTERVAL_TICKS.get();long started=System.nanoTime();PopulationSavedData data=PopulationSavedData.get(server);var batch=data.proposalBatch(data.proposalMaintenanceCursor(),CivitasConfig.MARRIAGE_MAINTENANCE_BATCH_SIZE.get());for(MarriageProposal proposal:batch.records()){examined++;if(proposal.status()==MarriageProposal.Status.PENDING&&now>=proposal.expiresAt()){data.putProposal(proposal.expire(now));expired++;}else if(proposal.status()!=MarriageProposal.Status.PENDING&&now-Math.max(proposal.createdAt(),proposal.expiresAt())>=CivitasConfig.MARRIAGE_TERMINAL_RETENTION_TICKS.get()){data.removeProposal(proposal.id());purged++;}}data.proposalMaintenanceCursor(batch.wrapped()?null:batch.nextCursor());batches++;long micros=Math.max(0,(System.nanoTime()-started)/1_000);totalMicros+=micros;maxBatchMicros=Math.max(maxBatchMicros,micros);}
    public static Metrics metrics(){return new Metrics(batches,examined,expired,purged,totalMicros,batches==0?0:totalMicros/batches,maxBatchMicros);}
    public record Metrics(long batches,long examined,long expired,long purged,long totalMicros,long averageBatchMicros,long maxBatchMicros){}
    private MarriageProposalMaintenanceManager(){}
}
