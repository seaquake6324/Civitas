package com.seaquake6324.civitas.infrastructure.spawn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class ServerRegionClassifierMetricsTest {
    @Test
    void usesFloorDivisionForNegativeCellCoordinates() {
        assertEquals(-2, ServerRegionClassifier.cellCoordinate(-5));
        assertEquals(-1, ServerRegionClassifier.cellCoordinate(-4));
        assertEquals(-1, ServerRegionClassifier.cellCoordinate(-1));
        assertEquals(0, ServerRegionClassifier.cellCoordinate(0));
        assertEquals(0, ServerRegionClassifier.cellCoordinate(3));
        assertEquals(1, ServerRegionClassifier.cellCoordinate(4));
    }

    @Test
    void timingWindowEvictsOldValuesAndRecomputesItsMaximum() {
        ServerRegionClassifier.RollingTimingWindow timings =
                new ServerRegionClassifier.RollingTimingWindow(3);
        timings.add(10);
        timings.add(20);
        timings.add(30);

        assertEquals(3, timings.size());
        assertEquals(20, timings.averageNanos());
        assertEquals(30, timings.maximumNanos());

        timings.add(5);
        timings.add(6);
        timings.add(7);

        assertEquals(3, timings.size());
        assertEquals(6, timings.averageNanos());
        assertEquals(7, timings.maximumNanos());
    }

    @Test
    void timingWindowRejectsInvalidCapacityAndClampsInvalidDurations() {
        assertThrows(IllegalArgumentException.class,
                () -> new ServerRegionClassifier.RollingTimingWindow(0));
        ServerRegionClassifier.RollingTimingWindow timings =
                new ServerRegionClassifier.RollingTimingWindow(2);
        timings.add(-1);

        assertEquals(0, timings.averageNanos());
        assertEquals(0, timings.maximumNanos());
    }
}
