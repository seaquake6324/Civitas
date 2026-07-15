package com.seaquake6324.civitas.domain.territory;
import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.domain.ChunkCoordinate;
import com.seaquake6324.civitas.domain.City;
import java.util.*;
import org.junit.jupiter.api.Test;
class TerritoryLifecycleTest {
 @Test void rejectsDiagonalAndPreventsArticulationRetraction(){
  long a=ChunkCoordinate.pack(0,0),b=ChunkCoordinate.pack(1,0),c=ChunkCoordinate.pack(2,0),diagonal=ChunkCoordinate.pack(1,1);
  assertFalse(TerritoryTopology.touches(Set.of(a),diagonal));
  assertFalse(TerritoryTopology.removable(Set.of(a,b,c),b,a));
  assertTrue(TerritoryTopology.removable(Set.of(a,b,c),c,a));
 }
 @Test void heartlandIsNeverRetractionCandidate(){
  long core=ChunkCoordinate.pack(0,0),edge=ChunkCoordinate.pack(1,0);
  Map<Long,TerritoryChunkState> states=new HashMap<>(); states.put(core,TerritoryChunkState.initial(0,true)); states.put(edge,TerritoryChunkState.initial(0,true).withNeglect(NeglectStage.RETRACTABLE,10));
  City city=new City(UUID.randomUUID(),"A",0,"minecraft:overworld",0,core,0,UUID.randomUUID(),UUID.randomUUID(),Set.of(),Set.of(core,edge),Set.of(core,edge),states,Map.of(),0);
  assertTrue(TerritoryRetraction.select(city,Map.of()).isEmpty());
 }
 @Test void recoveryNeedsTenContinuousMinutes(){
  var rules=new NeglectRules.Settings(20,20,72000,432000,1728000,12000);
  var abandoned=TerritoryChunkState.initial(0,false).withNeglect(NeglectStage.ABANDONED,100);
  var first=NeglectRules.evaluate(abandoned,new NeglectRules.Input(200,0,20,0,true,false,false,false,false),rules);
  assertEquals(NeglectStage.ABANDONED,first.state().neglectStage());
  var later=NeglectRules.evaluate(first.state(),new NeglectRules.Input(12200,0,20,0,true,false,false,false,false),rules);
  assertEquals(NeglectStage.WARNING,later.state().neglectStage());
 }
 @Test void interruptedRecoveryRestartsItsContinuousTimer(){
  var rules=new NeglectRules.Settings(20,20,100,200,300,50);
  var warning=TerritoryChunkState.initial(0,false).withNeglect(NeglectStage.WARNING,100);
  var recovering=NeglectRules.evaluate(warning,new NeglectRules.Input(150,0,20,0,true,false,false,false,false),rules);
  var interrupted=NeglectRules.evaluate(recovering.state(),new NeglectRules.Input(175,0,0,0,true,false,false,false,false),rules);
  assertEquals(0,interrupted.state().recoveryStartedAt());
  var resumed=NeglectRules.evaluate(interrupted.state(),new NeglectRules.Input(190,0,20,0,true,false,false,false,false),rules);
  assertEquals(190,resumed.state().recoveryStartedAt());
 }
 @Test void recordsTheMostRecentHealthyEvaluation(){var state=TerritoryChunkState.initial(10,false);var result=NeglectRules.evaluate(state,new NeglectRules.Input(40,25,25,0,true,false,false,false,false),new NeglectRules.Settings(20,20,100,200,300,50));assertEquals(40,result.state().lastHealthyAt());}
 @Test void retractionPrefersOldestThenLowestThenFarthest(){
  long core=ChunkCoordinate.pack(0,0),near=ChunkCoordinate.pack(1,0),far=ChunkCoordinate.pack(2,0);
  Map<Long,TerritoryChunkState> states=new HashMap<>();states.put(core,TerritoryChunkState.initial(0,true));
  states.put(near,new TerritoryChunkState(0,core,0,50,NeglectStage.RETRACTABLE,400,0,TerritoryChunkState.DefenseResult.NONE,0,false));
  states.put(far,new TerritoryChunkState(0,near,0,50,NeglectStage.RETRACTABLE,400,0,TerritoryChunkState.DefenseResult.NONE,0,false));
  City city=new City(UUID.randomUUID(),"A",0,"minecraft:overworld",0,core,0,UUID.randomUUID(),UUID.randomUUID(),Set.of(),Set.of(core,near,far),Set.of(core),states,Map.of(),0);
  assertEquals(far,TerritoryRetraction.select(city,Map.of(near,new TerritoryRetraction.Health(5,5),far,new TerritoryRetraction.Health(5,5))).orElseThrow());
 }
}
