package com.seaquake6324.civitas.domain.civilization;

import java.util.List;

/** Raw bounded-scan facts. Ratios are normalized here, while scoring remains in CivilityScoringRules. */
public record CivilityEvidence(int visitedCells, int standableCells, int passableCells,
        int largestConnectedCells, double enclosureRatio, int safePassableCells,
        int hazardousBoundaries, int protectedHazardousBoundaries,
        List<Integer> facilityDistributionPoints, int connectedFacilities,
        int territoryEdges, int connectedTerritoryEdges, boolean coreChunk,
        boolean coreConnected, BoundaryPorts ports) {
    public CivilityEvidence {
        visitedCells = Math.max(0, visitedCells);
        standableCells = Math.max(0, standableCells);
        passableCells = Math.max(0, passableCells);
        largestConnectedCells = Math.max(0, Math.min(passableCells, largestConnectedCells));
        enclosureRatio = ratio(enclosureRatio);
        safePassableCells = Math.max(0, Math.min(passableCells, safePassableCells));
        hazardousBoundaries = Math.max(0, hazardousBoundaries);
        protectedHazardousBoundaries = Math.max(0, Math.min(hazardousBoundaries, protectedHazardousBoundaries));
        facilityDistributionPoints = normalizeFacilities(facilityDistributionPoints);
        connectedFacilities = Math.max(0, Math.min(totalFacilities(facilityDistributionPoints), connectedFacilities));
        territoryEdges = Math.max(0, territoryEdges);
        connectedTerritoryEdges = Math.max(0, Math.min(territoryEdges, connectedTerritoryEdges));
        ports = ports == null ? BoundaryPorts.empty() : ports;
    }

    public static CivilityEvidence empty() {
        return new CivilityEvidence(0, 0, 0, 0, 0, 0, 0, 0,
                List.of(0, 0, 0, 0, 0, 0), 0, 0, 0, false, false, BoundaryPorts.empty());
    }
    public int totalFacilities() { return totalFacilities(facilityDistributionPoints); }
    private static List<Integer> normalizeFacilities(List<Integer> values) {
        Integer[] result = new Integer[FacilityCategory.values().length];
        for (int i = 0; i < result.length; i++) result[i] = values != null && i < values.size()
                ? Math.max(0, Math.min(3, values.get(i))) : 0;
        return List.of(result);
    }
    private static int totalFacilities(List<Integer> values) { return values.stream().mapToInt(Integer::intValue).sum(); }
    private static double ratio(double value) { return Math.max(0, Math.min(1, value)); }
}
