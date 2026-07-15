package com.seaquake6324.civitas.domain.border;
import static org.junit.jupiter.api.Assertions.*;import org.junit.jupiter.api.Test;
class InvasionWaveRulesTest{@Test void unloadedButRemainingEnemiesNeverCountAsVictory(){InvasionWaveRules rules=new InvasionWaveRules();assertEquals(InvasionWaveRules.Outcome.WAIT,rules.evaluate(4,1,100,200));assertEquals(InvasionWaveRules.Outcome.FAILURE,rules.evaluate(4,1,201,200));assertEquals(InvasionWaveRules.Outcome.ADVANCE,rules.evaluate(0,1,100,200));assertEquals(InvasionWaveRules.Outcome.SUCCESS,rules.evaluate(0,3,100,200));}}
