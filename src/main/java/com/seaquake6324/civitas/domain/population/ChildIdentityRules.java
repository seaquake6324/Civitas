package com.seaquake6324.civitas.domain.population;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Map;
import java.util.SplittableRandom;

/** Deterministic child identity choices; player parents currently contribute HUMAN. */
public final class ChildIdentityRules {
    public Result choose(PregnancyRecord pregnancy, Map<FamilyMemberRef,CitizenRace> parentRaces) {
        ArrayList<FamilyMemberRef> parents=new ArrayList<>(pregnancy.parents());
        parents.sort(Comparator.comparing((FamilyMemberRef ref)->ref.kind().ordinal()).thenComparing(FamilyMemberRef::id));
        long seed=pregnancy.id().getMostSignificantBits()^Long.rotateLeft(pregnancy.id().getLeastSignificantBits(),17);
        SplittableRandom random=new SplittableRandom(seed);
        FamilyMemberRef inheritedFrom=parents.get(random.nextInt(parents.size()));
        CitizenRace race=inheritedFrom.kind()==FamilyMemberRef.Kind.PLAYER?CitizenRace.HUMAN:parentRaces.get(inheritedFrom);
        if(race==null)throw new IllegalArgumentException("Missing NPC parent race");
        Gender gender=random.nextBoolean()?Gender.MALE:Gender.FEMALE;
        int appearance=race==CitizenRace.HUMAN?random.nextInt(4):0;
        return new Result(gender,race,race.name().toLowerCase(java.util.Locale.ROOT)+"_"+appearance,inheritedFrom);
    }
    public record Result(Gender gender,CitizenRace race,String appearanceKey,FamilyMemberRef inheritedFrom){}
}
