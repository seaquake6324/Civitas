package com.seaquake6324.civitas.domain.population;

import static org.junit.jupiter.api.Assertions.*;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ChildIdentityAndInfidelityRulesTest {
    @Test void childDeterministicallyInheritsExactlyOneParentAndPlayerCountsAsHuman(){
        FamilyMemberRef player=FamilyMemberRef.player(UUID.randomUUID()),npc=FamilyMemberRef.citizen(UUID.randomUUID());
        PregnancyRecord pregnancy=new PregnancyRecord(UUID.randomUUID(),UUID.randomUUID(),Set.of(player,npc),player,1,2,false,50,1);
        ChildIdentityRules rules=new ChildIdentityRules();ChildIdentityRules.Result first=rules.choose(pregnancy,Map.of(npc,CitizenRace.SHEEPFOLK)),second=rules.choose(pregnancy,Map.of(npc,CitizenRace.SHEEPFOLK));
        assertEquals(first,second);assertTrue(first.race()==CitizenRace.HUMAN||first.race()==CitizenRace.SHEEPFOLK);
        assertTrue(Set.of(player,npc).contains(first.inheritedFrom()));
    }

    @Test void infidelityMeansMarriedToAThirdPartyNotMerelyUnmarried(){
        FamilyMemberRef a=FamilyMemberRef.citizen(UUID.randomUUID()),b=FamilyMemberRef.citizen(UUID.randomUUID()),third=FamilyMemberRef.citizen(UUID.randomUUID());
        InfidelityRules rules=new InfidelityRules();
        assertFalse(rules.isInfidelity(a,b,Set.of(),Set.of()));
        assertFalse(rules.isInfidelity(a,b,Set.of(b),Set.of(a)));
        assertTrue(rules.isInfidelity(a,b,Set.of(third),Set.of()));
    }
}
