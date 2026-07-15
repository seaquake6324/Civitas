package com.seaquake6324.civitas.domain.population;

import java.util.Set;
import java.util.UUID;

/** Durable child origin; adoption never overwrites biological or migration provenance. */
public record ParentageRecord(UUID childId, UUID cityId, Set<FamilyMemberRef> parents,
                              long bornAt, boolean infidelity, Source source, long revision) {
    public ParentageRecord {
        if (childId == null || cityId == null || parents == null || source == null)
            throw new IllegalArgumentException("Missing parentage identity");
        parents = Set.copyOf(parents);
        if (parents.isEmpty() || parents.size() > 2 || bornAt < 0 || revision < 0)
            throw new IllegalArgumentException("Invalid parentage record");
        if (source == Source.BIRTH && parents.size() != 2)
            throw new IllegalArgumentException("Birth requires two recorded parents");
    }
    public enum Source { BIRTH, MIGRATION_WITH_SOURCE }
}
