package com.seaquake6324.civitas.infrastructure.activity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import com.seaquake6324.civitas.domain.civilization.ActivityWindowEvidence;
import org.junit.jupiter.api.Test;

class BoundedActivityWindowCacheTest {
    @Test void evictsTheOldestWindowAtTheCapacityBoundary() {
        BoundedActivityWindowCache<Integer> cache = new BoundedActivityWindowCache<>(2);
        cache.put(1, ActivityWindowEvidence.empty(1));
        cache.put(2, ActivityWindowEvidence.empty(2));
        cache.put(3, ActivityWindowEvidence.empty(3));
        assertEquals(2, cache.size());
        assertNull(cache.get(1));
        assertEquals(2, cache.get(2).windowStart());
    }

    @Test void movementRequiresAFullCellOfDistanceNotAJumpAcrossAGridBoundary() {
        assertFalse(ActivityManager.hasCrossedMovementCell(1, 2, 0, 7, 0, 0, 8, 0, 8));
        assertFalse(ActivityManager.hasCrossedMovementCell(1, 1, 0, 0, 0, 9, 0, 0, 8));
        assertTrue(ActivityManager.hasCrossedMovementCell(1, 2, 0, 0, 0, 8, 0, 0, 8));
    }
}
