package com.seaquake6324.civitas.domain.border;
import java.util.UUID;
public record Invasion(UUID id, BorderEdge origin, Phase phase, long phaseStartedAt, int wave, int alive, int spawned) {
    public enum Phase { OMEN, WARNING, WAVE, SUCCESS, FAILED, COOLDOWN }
    public Invasion { if(wave<0||wave>3||alive<0||spawned<0)throw new IllegalArgumentException(); }
}
