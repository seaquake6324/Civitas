package com.seaquake6324.civitas.domain.security;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GuardAssignmentRulesTest {
    private static final GuardAssignmentRules.Weights WEIGHTS=new GuardAssignmentRules.Weights(.35,.15,.20,.10,.05,.05,.10);
    @Test void keepsEveryExplainableComponentAndRewardsConfirmedEquipment(){
        var rules=new GuardAssignmentRules();
        var bare=rules.evaluate(new GuardAssignmentRules.Inputs(60,50,new GuardEquipment(false,0,false),GuardShift.NIGHT,50),WEIGHTS);
        var equipped=rules.evaluate(new GuardAssignmentRules.Inputs(60,50,new GuardEquipment(true,4,true),GuardShift.DAY,50),WEIGHTS);
        assertEquals(7,bare.components().size());assertTrue(equipped.score()>bare.score());assertEquals(20,equipped.components().get("weapon"));
    }
    @Test void fixedShiftsSplitVanillaDayTime(){assertTrue(GuardShift.DAY.onDuty(0));assertFalse(GuardShift.DAY.onDuty(12_000));assertFalse(GuardShift.NIGHT.onDuty(11_999));assertTrue(GuardShift.NIGHT.onDuty(23_999));}
}
