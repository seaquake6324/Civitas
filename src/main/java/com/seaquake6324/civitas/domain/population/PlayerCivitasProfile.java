package com.seaquake6324.civitas.domain.population;

import java.util.UUID;

public record PlayerCivitasProfile(UUID playerId,Gender gender,long revision){
    public PlayerCivitasProfile{revision=Math.max(0,revision);}
    public PlayerCivitasProfile withGender(Gender value){return new PlayerCivitasProfile(playerId,value,revision+1);}
}
