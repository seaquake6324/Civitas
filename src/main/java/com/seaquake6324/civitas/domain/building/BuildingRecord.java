package com.seaquake6324.civitas.domain.building;

import com.seaquake6324.civitas.domain.civilization.FacilityCategory;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Persistent, Minecraft-free authority for one validated building. */
public record BuildingRecord(UUID id, UUID cityId, String dimension, BuildingPurpose purpose,
        long entrance, long interior, Set<Long> cells, Map<FacilityCategory, Integer> facilities,
        BuildingFeatures features, int capacity, BuildingStatus status, long revision, long validatedAt, String invalidReason,
        Set<Long> authorizedStorageEndpoints) {
    public BuildingRecord {
        cells = Set.copyOf(cells);
        facilities = Map.copyOf(facilities);
        java.util.Objects.requireNonNull(features);
        authorizedStorageEndpoints = Set.copyOf(authorizedStorageEndpoints);
        invalidReason = invalidReason == null ? "" : invalidReason;
        if (cells.isEmpty() || capacity < 0 || revision < 0 || !features.storageEndpoints().containsAll(authorizedStorageEndpoints))
            throw new IllegalArgumentException("invalid building record");
    }

    public BuildingRecord(UUID id, UUID cityId, String dimension, BuildingPurpose purpose,
            long entrance, long interior, Set<Long> cells, Map<FacilityCategory, Integer> facilities,
            BuildingFeatures features, int capacity, BuildingStatus status, long revision, long validatedAt, String invalidReason) {
        this(id, cityId, dimension, purpose, entrance, interior, cells, facilities, features, capacity,
                status, revision, validatedAt, invalidReason, Set.of());
    }

    public BuildingRecord(UUID id, UUID cityId, String dimension, BuildingPurpose purpose,
            long entrance, long interior, Set<Long> cells, Map<FacilityCategory, Integer> facilities,
            int capacity, BuildingStatus status, long revision, long validatedAt, String invalidReason) {
        this(id, cityId, dimension, purpose, entrance, interior, cells, facilities,
                BuildingFeatures.EMPTY, capacity, status, revision, validatedAt, invalidReason, Set.of());
    }

    public BuildingRecord stale(String reason) {
        return status == BuildingStatus.STALE ? this : new BuildingRecord(id, cityId, dimension, purpose,
                entrance, interior, cells, facilities, features, capacity, BuildingStatus.STALE, revision + 1,
                validatedAt, reason, authorizedStorageEndpoints);
    }

    public BuildingRecord authorizeStorage(long endpoint, boolean authorized) {
        if (status != BuildingStatus.VALID || !features.storageEndpoints().contains(endpoint))
            throw new IllegalArgumentException("storage endpoint is not currently valid");
        java.util.HashSet<Long> next = new java.util.HashSet<>(authorizedStorageEndpoints);
        if (authorized) next.add(endpoint); else next.remove(endpoint);
        if (next.equals(authorizedStorageEndpoints)) return this;
        return new BuildingRecord(id, cityId, dimension, purpose, entrance, interior, cells, facilities,
                features, capacity, status, revision + 1, validatedAt, invalidReason, next);
    }
}
