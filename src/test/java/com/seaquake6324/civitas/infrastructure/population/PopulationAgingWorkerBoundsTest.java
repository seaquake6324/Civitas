package com.seaquake6324.civitas.infrastructure.population;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.domain.population.*;
import java.util.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PopulationAgingWorkerBoundsTest {
    @AfterEach void close(){PopulationAgingManager.shutdownWorkerForTest();}

    @Test void productionWorkerAppliesBackpressureAndDeduplicatesInFlightCitizens() throws Exception {
        PopulationAgingManager.initializeWorkerForTest(1,1,2);
        CitizenSimulationSnapshot first=snapshot(new UUID(0,1));
        assertTrue(PopulationAgingManager.submitSnapshotForTest(first));
        PopulationAgingManager.submitSnapshotForTest(first);
        for(int i=2;i<100;i++)PopulationAgingManager.submitSnapshotForTest(snapshot(new UUID(0,i)));
        long deadline=System.nanoTime()+2_000_000_000L;PopulationAgingManager.Metrics metrics;
        do{metrics=PopulationAgingManager.metrics();if(metrics.taskRejected()+metrics.resultDropped()>0&&metrics.duplicateInFlight()>0)break;Thread.onSpinWait();}while(System.nanoTime()<deadline);
        assertTrue(metrics.taskRejected()+metrics.resultDropped()>0,"bounded queues must produce observable backpressure");
        assertTrue(metrics.duplicateInFlight()>0);
        assertTrue(metrics.workQueue()<=1);assertTrue(metrics.intentQueue()<=2);assertTrue(metrics.inFlight()<=4);
    }

    private static CitizenSimulationSnapshot snapshot(UUID id){UUID city=UUID.randomUUID();CitizenRecord citizen=new CitizenRecord(id,"Test","Citizen",CitizenRace.HUMAN,"human_0",Gender.MALE,1800,100,city,null,null,null,"",100,CitizenNeeds.neutral(),Map.of(),Set.of(),50,0,CitizenRuntimeState.VIRTUAL,0,70,0,1);return new CitizenSimulationSnapshot(citizen,city,0,List.of(),200,new AgeRules(100,12,18,35,60),new LifespanRules(60,90));}
}
