package com.seaquake6324.civitas.application.port;

import com.seaquake6324.civitas.domain.building.BuildingRecord;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;

public interface BuildingRepository {
    Collection<BuildingRecord> forCity(UUID cityId);
    default boolean overlaps(UUID cityId, String dimension, Set<Long> cells, UUID excludedId) {
        return forCity(cityId).stream().filter(record -> excludedId == null || !record.id().equals(excludedId))
                .filter(record -> record.dimension().equals(dimension))
                .anyMatch(record -> record.cells().stream().anyMatch(cells::contains));
    }
    Optional<BuildingRecord> byId(UUID id);
    void put(BuildingRecord building);
}
