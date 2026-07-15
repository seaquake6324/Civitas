package com.seaquake6324.civitas.infrastructure.network;

import java.util.Set;
import java.util.UUID;

public record CityMapRecord(UUID id, String name, int color, String dimension, String lordName, Set<Long> territory) {
    public CityMapRecord {
        territory = Set.copyOf(territory);
    }
}
