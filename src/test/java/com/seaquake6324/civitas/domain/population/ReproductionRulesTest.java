package com.seaquake6324.civitas.domain.population;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReproductionRulesTest {
    private final ReproductionRules rules=new ReproductionRules();
    private static final ReproductionRules.Conditions FULL=new ReproductionRules.Conditions(100,100,100,100,100,100,100,100);

    @Test void usesConfirmedStageModifiersAndYoungBaselineForPlayers(){
        assertEquals(0,rules.fertility(AgeStage.CHILD));assertEquals(.7,rules.fertility(AgeStage.ADOLESCENT));
        assertEquals(1,rules.fertility(AgeStage.YOUNG_ADULT));assertEquals(.8,rules.fertility(AgeStage.MATURE_ADULT));
        assertEquals(.5,rules.fertility(AgeStage.ELDER));assertEquals(1,rules.fertility(null));
    }

    @Test void marriageIsNotAnEligibilityInputAndConditionsRemainSoft(){
        var male=new ReproductionRules.Participant(FamilyMemberRef.citizen(UUID.randomUUID()),Gender.MALE,AgeStage.ADOLESCENT,true,100);
        var female=new ReproductionRules.Participant(FamilyMemberRef.citizen(UUID.randomUUID()),Gender.FEMALE,AgeStage.ELDER,true,100);
        var full=rules.evaluate(male,female,FULL);
        var poor=rules.evaluate(male,female,new ReproductionRules.Conditions(0,0,0,0,0,0,0,0));
        assertTrue(full.biologicallyEligible());assertEquals(.35,full.stageModifier(),1e-9);
        assertEquals(35,full.effectiveWillingness(),1e-9);
        assertTrue(poor.biologicallyEligible());assertEquals(0,poor.effectiveWillingness());
        assertTrue(poor.attemptIntervalTicks()>full.attemptIntervalTicks());
    }

    @Test void sameGenderChildAndDeadParticipantsAreBiologicallyIneligible(){
        var a=new ReproductionRules.Participant(FamilyMemberRef.player(UUID.randomUUID()),Gender.FEMALE,null,true,100);
        var b=new ReproductionRules.Participant(FamilyMemberRef.citizen(UUID.randomUUID()),Gender.FEMALE,AgeStage.YOUNG_ADULT,true,100);
        assertFalse(rules.evaluate(a,b,FULL).biologicallyEligible());
        var child=new ReproductionRules.Participant(FamilyMemberRef.citizen(UUID.randomUUID()),Gender.MALE,AgeStage.CHILD,true,100);
        assertFalse(rules.evaluate(a,child,FULL).biologicallyEligible());
    }
}
