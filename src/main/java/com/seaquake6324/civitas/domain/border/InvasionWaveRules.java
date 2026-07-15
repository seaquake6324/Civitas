package com.seaquake6324.civitas.domain.border;
/** Pure wave lifecycle; loaded entity visibility is deliberately not an input. */
public final class InvasionWaveRules{public Outcome evaluate(int remainingMobs,int wave,long elapsedTicks,long timeLimitTicks){if(remainingMobs<0||wave<1||elapsedTicks<0||timeLimitTicks<1)throw new IllegalArgumentException("invalid wave state");if(remainingMobs==0)return wave>=3?Outcome.SUCCESS:Outcome.ADVANCE;if(elapsedTicks>timeLimitTicks)return Outcome.FAILURE;return Outcome.WAIT;}public enum Outcome{WAIT,ADVANCE,SUCCESS,FAILURE}}
