package com.seaquake6324.civitas.domain.population;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class MaterializationRulesTest{
 private final MaterializationRules rules=new MaterializationRules(48,64,72);
 @Test void prewarmsBeforeMaterializingAndUsesExitHysteresis(){assertEquals(CitizenRuntimeState.VIRTUAL,rules.evaluate(CitizenRuntimeState.VIRTUAL,90,true,false,false).state());assertEquals(CitizenRuntimeState.PREWARMING,rules.evaluate(CitizenRuntimeState.VIRTUAL,60,true,false,false).state());assertEquals(CitizenRuntimeState.MATERIALIZED,rules.evaluate(CitizenRuntimeState.PREWARMING,40,true,false,false).state());assertEquals(CitizenRuntimeState.MATERIALIZED,rules.evaluate(CitizenRuntimeState.MATERIALIZED,70,true,true,false).state());assertEquals(CitizenRuntimeState.VIRTUAL,rules.evaluate(CitizenRuntimeState.MATERIALIZED,73,true,true,false).state());}
 @Test void validNodeAndLocksAreExplicit(){assertEquals("no_valid_node",rules.evaluate(CitizenRuntimeState.VIRTUAL,1,false,false,false).reason());assertEquals(CitizenRuntimeState.LOCKED,rules.evaluate(CitizenRuntimeState.MATERIALIZED,100,true,true,true).state());assertEquals(CitizenRuntimeState.MATERIALIZED,rules.evaluate(CitizenRuntimeState.LOCKED,40,true,true,false).state());}
 @Test void rejectsNonMonotonicDistances(){assertThrows(IllegalArgumentException.class,()->new MaterializationRules(64,48,72));assertThrows(IllegalArgumentException.class,()->new MaterializationRules(48,64,60));}
}
