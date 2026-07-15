package com.seaquake6324.civitas.domain.border;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

class ThreatPressureTest {
    @Test void lowReadinessAndLargerTerritoryProduceExplainablePressure() {
        var low = BorderReadiness.calculate(10,10,10,10);
        var high = BorderReadiness.calculate(90,90,90,90);
        var start = new ThreatPressure(0,0,0,0);
        var lowUpdate = start.accumulate(low,25,20,1,.08);
        var highUpdate = start.accumulate(high,4,20,1,.08);
        assertTrue(lowUpdate.pressure().value()>highUpdate.pressure().value());
        assertEquals(lowUpdate.breakdown().readinessGain()+lowUpdate.breakdown().citySizeGain(),
                lowUpdate.breakdown().finalGain(),.00001);
    }

    @Test void successfulDefenseBufferCancelsAccumulation() {
        var buffered = new ThreatPressure(50,0,100,0).accumulate(
                BorderReadiness.calculate(0,0,0,0),9,20,1,.08);
        assertEquals(50,buffered.pressure().value(),.00001);
        assertEquals(0,buffered.breakdown().finalGain(),.00001);
    }
}
