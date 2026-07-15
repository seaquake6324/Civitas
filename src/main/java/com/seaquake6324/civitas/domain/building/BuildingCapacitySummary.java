package com.seaquake6324.civitas.domain.building;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/** Population and security consume this summary instead of re-reading building rules. */
public record BuildingCapacitySummary(int validBuildings, int staleBuildings, int invalidBuildings, int housingCapacity,
        int guardCapacity, Map<BuildingPurpose, Integer> capacityByPurpose) {
    public BuildingCapacitySummary { capacityByPurpose = Map.copyOf(capacityByPurpose); }

    public static BuildingCapacitySummary from(Collection<BuildingRecord> records) {
        int valid = 0, stale = 0, invalid = 0, housing = 0, guards = 0;
        EnumMap<BuildingPurpose, Integer> byPurpose = new EnumMap<>(BuildingPurpose.class);
        for (BuildingRecord record : records) {
            if (record.status() == BuildingStatus.STALE) { stale++; continue; }
            if (record.status() == BuildingStatus.INVALID) { invalid++; continue; }
            valid++;
            byPurpose.merge(record.purpose(), record.capacity(), Integer::sum);
            if (record.purpose() == BuildingPurpose.RESIDENCE) housing += record.capacity();
            if (record.purpose() == BuildingPurpose.GUARD_POST) guards += record.capacity();
        }
        return new BuildingCapacitySummary(valid, stale, invalid, housing, guards, byPurpose);
    }
}
