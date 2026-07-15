package com.seaquake6324.civitas.domain.population;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class OutMigrationRulesTest{
 @Test void pressureIsExplainableAndBoundedByConfiguredMaximum(){var rules=new OutMigrationRules();var stable=rules.evaluate(new OutMigrationRules.Inputs(0,100,100,100,100,100,100),.2);assertEquals(0,stable.score(),1e-9);assertEquals(0,stable.chance(),1e-9);var pressured=rules.evaluate(new OutMigrationRules.Inputs(100,0,0,0,0,0,0),.2);assertEquals(100,pressured.score(),1e-9);assertEquals(.2,pressured.chance(),1e-9);assertEquals(7,pressured.components().size());assertEquals(35,pressured.components().get("migration_willingness"),1e-9);}
 @Test void eachInputMovesOnlyItsNamedComponent(){var rules=new OutMigrationRules();var result=rules.evaluate(new OutMigrationRules.Inputs(80,20,30,40,50,60,70),.1);assertEquals(28,result.components().get("migration_willingness"),1e-9);assertEquals(12,result.components().get("settlement_doubt"),1e-9);assertTrue(result.chance()>=0&&result.chance()<=.1);}
}
