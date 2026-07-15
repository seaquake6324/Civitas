package com.seaquake6324.civitas.infrastructure.civilization;

import static org.junit.jupiter.api.Assertions.*;
import com.seaquake6324.civitas.domain.civilization.CivilizationLayer;
import org.junit.jupiter.api.Test;

class CivilityDirtyQueueTest {
    @Test void deduplicatesAndMergesReasons() {
        CivilityDirtyQueue q = new CivilityDirtyQueue(2);
        var key = new CivilityDirtyQueue.Key("overworld", 1, CivilizationLayer.SURFACE);
        assertTrue(q.offer(key, 10, CivilityDirtyQueue.Reason.BLOCK_CHANGE));
        assertTrue(q.offer(key, 20, CivilityDirtyQueue.Reason.BORDER_CHANGE));
        assertEquals(1, q.size());
        assertEquals(2, Integer.bitCount(q.poll(ignored -> 0).orElseThrow().reasons()));
    }

    @Test void prioritizesDistanceThenOldestAndReportsOverflow() {
        CivilityDirtyQueue q = new CivilityDirtyQueue(2);
        var oldFar = new CivilityDirtyQueue.Key("d", 1, CivilizationLayer.SURFACE);
        var newNear = new CivilityDirtyQueue.Key("d", 2, CivilizationLayer.SURFACE);
        q.offer(oldFar, 1, CivilityDirtyQueue.Reason.BLOCK_CHANGE);
        q.offer(newNear, 2, CivilityDirtyQueue.Reason.BLOCK_CHANGE);
        assertFalse(q.offer(new CivilityDirtyQueue.Key("d", 3, CivilizationLayer.SURFACE), 3,
                CivilityDirtyQueue.Reason.BLOCK_CHANGE));
        assertEquals(newNear, q.poll(key -> key.equals(newNear) ? 0 : 10).orElseThrow().key());
        assertEquals(oldFar, q.poll(ignored -> 0).orElseThrow().key());
    }
}
