package com.seaquake6324.civitas.domain.building;

import java.util.Set;

/** Raw, explainable endpoints found by the bounded world scan; no inventory policy is implied. */
public record BuildingFeatures(Set<Long> boundaryPorts, Set<Long> workstations,
        Set<Long> storageEndpoints, boolean entranceConnected) {
    public static final BuildingFeatures EMPTY = new BuildingFeatures(Set.of(), Set.of(), Set.of(), false);

    public BuildingFeatures {
        boundaryPorts = Set.copyOf(boundaryPorts);
        workstations = Set.copyOf(workstations);
        storageEndpoints = Set.copyOf(storageEndpoints);
    }
}
