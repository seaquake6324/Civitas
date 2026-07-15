package com.seaquake6324.civitas.domain.territory;

/** Authoritative, Minecraft-independent lifecycle metadata for one claimed X/Z chunk. */
public record TerritoryChunkState(long claimedAt, Long sourceChunk, long lastHealthyAt,
        long neglectStartedAt, NeglectStage neglectStage, long stageChangedAt,
        long recoveryStartedAt, DefenseResult lastDefenseResult, long lastDefenseAt,
        boolean permanentHeartland) {
    public enum DefenseResult { NONE, SUCCESS, FAILURE }

    public TerritoryChunkState {
        if (claimedAt < 0 || lastHealthyAt < 0 || neglectStartedAt < 0 || stageChangedAt < 0
                || recoveryStartedAt < 0 || lastDefenseAt < 0) throw new IllegalArgumentException("negative territory time");
        if (neglectStage == null || lastDefenseResult == null) throw new IllegalArgumentException("missing lifecycle state");
    }

    public static TerritoryChunkState initial(long now, boolean heartland) {
        return new TerritoryChunkState(now, null, now, 0, NeglectStage.HEALTHY, now, 0,
                DefenseResult.NONE, 0, heartland);
    }

    public static TerritoryChunkState expansion(long now, long sourceChunk) {
        return new TerritoryChunkState(now, sourceChunk, now, 0, NeglectStage.HEALTHY, now, 0,
                DefenseResult.NONE, 0, false);
    }

    public TerritoryChunkState withNeglect(NeglectStage stage, long now) {
        return new TerritoryChunkState(claimedAt, sourceChunk, lastHealthyAt,
                stage == NeglectStage.HEALTHY ? 0 : (neglectStartedAt == 0 ? now : neglectStartedAt),
                stage, now, 0, lastDefenseResult, lastDefenseAt, permanentHeartland);
    }

    public TerritoryChunkState withRecoveryStarted(long now) {
        return recoveryStartedAt == 0 ? new TerritoryChunkState(claimedAt, sourceChunk, lastHealthyAt,
                neglectStartedAt, neglectStage, stageChangedAt, now, lastDefenseResult, lastDefenseAt,
                permanentHeartland) : this;
    }
    public TerritoryChunkState withLastHealthyAt(long now) {
        if (now <= lastHealthyAt) return this;
        return new TerritoryChunkState(claimedAt, sourceChunk, now, neglectStartedAt, neglectStage,
                stageChangedAt, recoveryStartedAt, lastDefenseResult, lastDefenseAt, permanentHeartland);
    }
    public TerritoryChunkState pauseTimers(long ticks){
        if(ticks<=0)return this;return new TerritoryChunkState(claimedAt,sourceChunk,lastHealthyAt,
                neglectStartedAt==0?0:neglectStartedAt+ticks,neglectStage,stageChangedAt,
                recoveryStartedAt==0?0:recoveryStartedAt+ticks,lastDefenseResult,lastDefenseAt,permanentHeartland);
    }
    public TerritoryChunkState withDefense(DefenseResult result,long now){return new TerritoryChunkState(claimedAt,sourceChunk,lastHealthyAt,neglectStartedAt,neglectStage,stageChangedAt,recoveryStartedAt,result,now,permanentHeartland);}
}
