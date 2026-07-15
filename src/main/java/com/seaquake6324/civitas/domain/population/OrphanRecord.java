package com.seaquake6324.civitas.domain.population;

import java.util.UUID;

/** A real existing child awaiting local adoption; this record never creates a citizen. */
public record OrphanRecord(UUID childId, UUID cityId, Reason reason, UUID originHouseholdId,
                          UUID lastResidenceId, long admittedAt, long revision) {
    public OrphanRecord {
        if (childId == null || cityId == null || reason == null || admittedAt < 0 || revision < 0)
            throw new IllegalArgumentException("Invalid orphan record");
    }
    public enum Reason { PARENTS_UNAVAILABLE, SEPARATED_FROM_FAMILY, MIGRANT_WITH_SOURCE }
}
