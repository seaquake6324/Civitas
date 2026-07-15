package com.seaquake6324.civitas.domain.population;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Persisted multi-player consent for adopting one existing orphan. */
public record AdoptionConsent(UUID id,UUID cityId,UUID childId,UUID householdId,Set<FamilyMemberRef>guardians,
        Set<UUID>requiredPlayers,Set<UUID>confirmedPlayers,UUID residenceId,String anchorDimension,long anchorPosition,
        long createdAt,long expiresAt,long revision){
    public AdoptionConsent{guardians=Set.copyOf(guardians);requiredPlayers=Set.copyOf(requiredPlayers);confirmedPlayers=Set.copyOf(confirmedPlayers);if(id==null||cityId==null||childId==null||householdId==null||guardians.isEmpty()||guardians.size()>2||requiredPlayers.isEmpty()||!requiredPlayers.containsAll(confirmedPlayers)||anchorDimension==null||anchorDimension.isBlank()||createdAt<0||expiresAt<=createdAt||revision<1)throw new IllegalArgumentException("invalid adoption consent");Set<UUID>players=new HashSet<>();for(FamilyMemberRef guardian:guardians)if(guardian.kind()==FamilyMemberRef.Kind.PLAYER)players.add(guardian.id());if(!players.equals(requiredPlayers))throw new IllegalArgumentException("player guardian set mismatch");}
    public AdoptionConsent confirm(UUID player){if(!requiredPlayers.contains(player))throw new IllegalArgumentException("not a required guardian");Set<UUID>next=new HashSet<>(confirmedPlayers);next.add(player);return new AdoptionConsent(id,cityId,childId,householdId,guardians,requiredPlayers,next,residenceId,anchorDimension,anchorPosition,createdAt,expiresAt,revision+1);}
    public boolean complete(){return confirmedPlayers.equals(requiredPlayers);}
}
