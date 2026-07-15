package com.seaquake6324.civitas.domain.population;

import java.util.UUID;

/** Deterministic per-citizen old-age limit; restart and iteration order cannot reroll it. */
public record LifespanRules(int minimumYears,int maximumYears){
    public LifespanRules{if(minimumYears<1||maximumYears<minimumYears)throw new IllegalArgumentException("invalid lifespan range");}
    public int years(UUID citizenId){long mixed=citizenId.getMostSignificantBits()^Long.rotateLeft(citizenId.getLeastSignificantBits(),29);mixed^=mixed>>>33;mixed*=0xff51afd7ed558ccdl;mixed^=mixed>>>33;return minimumYears+Math.floorMod(mixed,maximumYears-minimumYears+1);}
    public boolean expired(CitizenRecord citizen,AgeRules ageRules){return citizen.alive()&&ageRules.evaluate(citizen.ageTicks()).completedYears()>=citizen.lifespanYears();}
}
