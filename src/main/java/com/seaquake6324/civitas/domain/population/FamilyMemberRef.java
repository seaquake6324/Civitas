package com.seaquake6324.civitas.domain.population;

import java.util.UUID;

/** Typed identity prevents a player UUID and citizen UUID from becoming ambiguous in a household. */
public record FamilyMemberRef(Kind kind,UUID id){
    public enum Kind{PLAYER,CITIZEN}
    public FamilyMemberRef{if(kind==null||id==null)throw new IllegalArgumentException("missing family member identity");}
    public static FamilyMemberRef player(UUID id){return new FamilyMemberRef(Kind.PLAYER,id);}
    public static FamilyMemberRef citizen(UUID id){return new FamilyMemberRef(Kind.CITIZEN,id);}
}
