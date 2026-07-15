package com.seaquake6324.civitas.domain.territory;

public final class NeglectRules {
    public record Settings(double civilityMinimum,double activityMinimum,long warningTicks,long abandonedTicks,
            long retractableTicks,long recoveryTicks){}
    public record Input(long now,double civility,double targetCivility,double activity,boolean border,boolean core,
            boolean heartland,boolean invasionOrRecovery,boolean validActivity){}
    public record Result(TerritoryChunkState state,long remaining,String reason){}
    public static Result evaluate(TerritoryChunkState state,Input in,Settings rules){
        if(in.core||in.heartland||!in.border)return new Result(state,0,"protected");
        boolean recovered=in.activity>=rules.activityMinimum||in.targetCivility>=rules.civilityMinimum||in.validActivity;
        if(recovered){
            if(state.neglectStage()==NeglectStage.HEALTHY)return new Result(state.withLastHealthyAt(in.now),0,"healthy");
            TerritoryChunkState tracking=state.withRecoveryStarted(in.now);
            long elapsed=in.now-tracking.recoveryStartedAt();
            if(elapsed>=rules.recoveryTicks){NeglectStage recoveredStage=tracking.neglectStage().recover();long resumed=recoveredStage==NeglectStage.HEALTHY?0:Math.max(1,in.now-stageStart(recoveredStage,rules));TerritoryChunkState next=new TerritoryChunkState(tracking.claimedAt(),tracking.sourceChunk(),in.now,resumed,recoveredStage,in.now,0,tracking.lastDefenseResult(),tracking.lastDefenseAt(),tracking.permanentHeartland());return new Result(next,0,"stage_recovered");}
            return new Result(tracking,rules.recoveryTicks-elapsed,"recovering");
        }
        if(in.invasionOrRecovery)return new Result(state,0,"paused");
        boolean low=in.civility<rules.civilityMinimum&&in.activity<rules.activityMinimum;
        if(!low)return new Result(state.withLastHealthyAt(in.now),0,"not_both_low");
        if(state.recoveryStartedAt()>0)state=new TerritoryChunkState(state.claimedAt(),state.sourceChunk(),state.lastHealthyAt(),state.neglectStartedAt(),state.neglectStage(),state.stageChangedAt(),0,state.lastDefenseResult(),state.lastDefenseAt(),state.permanentHeartland());
        long started=state.neglectStartedAt()==0?in.now:state.neglectStartedAt(); long elapsed=in.now-started;
        NeglectStage wanted=elapsed>=rules.retractableTicks?NeglectStage.RETRACTABLE:elapsed>=rules.abandonedTicks?NeglectStage.ABANDONED:elapsed>=rules.warningTicks?NeglectStage.WARNING:NeglectStage.HEALTHY;
        TerritoryChunkState next=state;
        if(state.neglectStartedAt()==0)next=new TerritoryChunkState(state.claimedAt(),state.sourceChunk(),state.lastHealthyAt(),started,state.neglectStage(),state.stageChangedAt(),0,state.lastDefenseResult(),state.lastDefenseAt(),state.permanentHeartland());
        if(wanted.ordinal()>next.neglectStage().ordinal())next=next.withNeglect(wanted,in.now);
        long threshold=switch(wanted){case HEALTHY->rules.warningTicks;case WARNING->rules.abandonedTicks;case ABANDONED->rules.retractableTicks;case RETRACTABLE->elapsed;};
        return new Result(next,Math.max(0,threshold-elapsed),"low_civility_and_activity");
    }
    private static long stageStart(NeglectStage stage,Settings rules){return switch(stage){case HEALTHY->0;case WARNING->rules.warningTicks;case ABANDONED->rules.abandonedTicks;case RETRACTABLE->rules.retractableTicks;};}
    private NeglectRules(){}
}
